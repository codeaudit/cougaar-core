/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.service.wp;

import java.io.Serializable;

/**
 * Request for the white pages service.
 * <p>
 * Request objects are immutable.  A client submits a request
 * to the white pages and watches the mutable Response object.
 */
public abstract class Request implements Serializable {

  /**
   * The options flag to indicate no options.
   */
  public static final int NONE = 0;

  /**
   * Flag to bypass the cache for "get", "getAll", and "list"
   * requests.
   * <p>
   * This should only be enabled if the client has detected a
   * <b>strong</b> out-of-band hint that the entry is stale, such
   * as a lost network connection to a URI returned by a prior
   * "get" request.
   * <p>
   * The cache is free to ignore this request if it has just fetched
   * the value -- the minimum entry TTL for a bypass is a cache
   * configuration option.
   */
  public static final int BYPASS_CACHE = (1<<0);

  /**
   * Flag to limit the operation to the local cache.
   * <p>
   * For "get", "getAll", and "list", a CACHE_ONLY flag limits the
   * lookup to the local cache.  If the result is not in the cache
   * then the result will be set to the default value as defined
   * below.
   * <p>
   * For "bind", this can be used to bootstrap entries into the local
   * (client-side) "get" table.  This can be used for both the local
   * agents and remote agents discovered through non-WP mechanisms.
   * If a future "get" request is not in the cache, then the hints
   * are checked and will be used if present.  A hint can be removed
   * with an "unbind-hint".
   */
  public static final int CACHE_ONLY   = (1<<1);
  // todo: recurse, authority-only, etc

  private final int options;

  private Request(int options) {
    this.options = options;
  }

  public final boolean hasOption(int mask) {
    return ((options & mask) != 0);
  }

  /**
   * Create a response object for this request.
   */
  public abstract Response createResponse();

  public String toString() {
    return 
      " oid="+System.identityHashCode(this)+
      " bypassCache="+hasOption(BYPASS_CACHE)+
      " cacheOnly="+hasOption(CACHE_ONLY)+
      ")";
  }

  /**
   * Get the entry with the matching (name, type) fields.
   *
   * @see Request.GetAll get all entries with a given name
   */
  public static final class Get extends Request {
    private final String name;
    private final String type;
    private transient int _hc;
    public Get(
        int options,
        String name,
        String type) {
      super(options);
      this.name = name;
      this.type = type;
      if (name==null || type==null) {
        throw new IllegalArgumentException("Null parameter");
      }
    }
    public String getName() { return name; }
    public String getType() { return type; }
    public Response createResponse() {
      return new Response.Get(this);
    }
    public int hashCode() {
      if (_hc == 0) {
        int h = 0;
        h = 31*h + name.hashCode();
        h = 31*h + type.hashCode();
        _hc = h;
      }
      return _hc;
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Get)) {
        return false;
      } else {
        Get g = (Get) o;
        return
          (name.equals(g.name) &&
           type.equals(g.type));
      }
    }
    public String toString() {
      return 
        "("+
        (hasOption(BYPASS_CACHE) ? "refresh" : "get")+
        " name="+getName()+
        " type="+getType()+
        super.toString();
    }
  }

  /**
   * Get all entries associated with the given name.
   *
   * @see Request.Get do a specific (name, type) lookup
   */
  public static final class GetAll extends Request {
    private final String name;
    public GetAll(
        int options,
        String name) {
      super(options);
      this.name = name;
      if (name == null) {
        throw new IllegalArgumentException("null name");
      }
    }
    public String getName() { 
      return name;
    }
    public Response createResponse() {
      return new Response.GetAll(this);
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof GetAll)) {
        return false;
      } else {
        return name.equals(((GetAll) o).name);
      }
    }
    public int hashCode() {
      return name.hashCode();
    }
    public String toString() {
      return 
        "("+
        (hasOption(BYPASS_CACHE) ? "refreshAll" : "getAll")+
        " name="+getName()+
        super.toString();
    }
  }

  /**
   * List the name of all direct children with the given suffix.
   * <p>
   * The suffix must start with '.'.  For names other than '.',
   * trailing '.'s will be ignored (e.g. ".x." is the same as
   * ".x").
   * <p>
   * The name space is separated by using the "." character, just
   * like internet host names (RFC 952).  All children of a name
   * must have a matching suffix, and only the direct (first-level)
   * children will be listed.
   * <p>
   * For example, given:<pre>
   *    list(".foo.com")
   * </pre>the result may look like be:<pre>
   *    { "www.foo.com", ".bar.foo.com" }
   * </pre>where "www.foo.com" is an entry, and ".bar.foo.com" is a
   * suffix for one or more child entries.  If there were entries
   * for both "www.foo.com" and subchild "test.www.foo.com", then both
   * would be listed:<pre>
   *    { "www.foo.com", ".www.foo.com", ".bar.foo.com" }
   * </pre>Note that listing ".foo.com" will not list the second
   * level children, such as "test.www.foo.com".  The client must
   * do the <i>(non-scalable)</i> depth traversal itself.
   * <p>
   * The precise regex pattern is:<pre>
   *     new java.util.regex.Pattern(
   *       "^\.?[^\.]+" +
   *       suffix +
   *       "$");</pre>
   * <p>
   * This is similar to a DNS zone transfer (AXFR) limited to depth=1.
   */
  public static final class List extends Request {
    private final String suffix;
    public List(
        int options,
        String suffix) {
      super(options);
      String suf = suffix;
      // must start with '.'
      int len = (suf == null ? 0 : suf.length());
      if (len == 0 || suf.charAt(0) != '.') {
        throw new IllegalArgumentException(
            "Suffix must start with '.': "+suf);
      }
      // trim tail '.'s
      while (--len > 0 && suf.charAt(len) == '.') {
        suf = suf.substring(0, len);
      }
      this.suffix = suf;
    }
    public String getSuffix() { 
      return suffix;
    }
    public Response createResponse() {
      return new Response.List(this);
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof List)) {
        return false;
      } else {
        return suffix.equals(((List) o).suffix);
      }
    }
    public int hashCode() {
      return suffix.hashCode();
    }
    public String toString() {
      return
        "("+
        (hasOption(BYPASS_CACHE) ? "relist" : "list")+
        " suffix="+getSuffix()+
        super.toString();
    }
  }

  /**
   * Bind a new entry, or rebind an existing entry if the overwrite
   * flag is false.
   * <p>
   * See the above notes on the CACHE_ONLY flag for binding
   * client-side bootstrap "hints".
   * <p>
   * The renewal flag is for the infrastructure's use, for renewing
   * bind leases.
   */
  public static final class Bind extends Request {

    private final AddressEntry ae;
    private final boolean overWrite;
    private final boolean renewal;

    public Bind(
        int options,
        AddressEntry ae,
        boolean overWrite,
        boolean renewal) {
      super(options);
      this.ae = ae;
      this.overWrite = overWrite;
      this.renewal = renewal;
      if (hasOption(BYPASS_CACHE)) {
        throw new IllegalArgumentException(
            "Bind can't \"BYPASS_CACHE\"");
      }
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
      if (renewal && (overWrite || hasOption(CACHE_ONLY))) {
        throw new IllegalArgumentException(
            "Renewal implies non-overwrite and non-cache-only");
      }
    }
    public AddressEntry getAddressEntry() { 
      return ae;
    }
    public boolean isOverWrite() {
      return overWrite;
    }
    public boolean isRenewal() {
      return renewal;
    }
    public Response createResponse() {
      return new Response.Bind(this);
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Bind)) {
        return false;
      } else {
        Bind b = (Bind) o;
        return 
          (ae.equals(b.ae) &&
           overWrite == b.overWrite &&
           renewal == b.renewal);
      }
    }
    public int hashCode() {
      return 
        (ae.hashCode() + 
         (overWrite ? 1 : 2) +
         (renewal ? 3 : 4));
    }
    public String toString() {
      return 
        "("+
        (hasOption(CACHE_ONLY) ? "hint_" : "")+
        (isOverWrite() ? "rebind" :
         isRenewal() ? "renew" :
         "bind")+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }

  /**
   * Destroy the binding for the specified entry.
   * <p>
   * The client must pass the current value for the bound entry.
   */
  public static final class Unbind extends Request {
    private final AddressEntry ae;
    public Unbind(
        int options,
        AddressEntry ae) {
      super(options);
      this.ae = ae;
      if (hasOption(BYPASS_CACHE)) {
        throw new IllegalArgumentException(
            "Unbind can't \"BYPASS_CACHE\"");
      }
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public AddressEntry getAddressEntry() {
      return ae;
    }
    public Response createResponse() {
      return new Response.Unbind(this);
    }
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      } else if (!(o instanceof Unbind)) {
        return false;
      } else {
        Unbind u = (Unbind) o;
        return ae.equals(u.ae);
      }
    }
    public int hashCode() {
      return ae.hashCode();
    }
    public String toString() {
      return 
        "("+
        (hasOption(CACHE_ONLY) ? "unhint" : "unbind")+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }
}
