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

  private final long timeout;
  private final boolean useCache;

  private Request(boolean useCache, long timeout) {
    this.useCache = useCache;
    this.timeout = timeout;
  }

  /**
   * Returns false if this request would prefer to bypass the
   * cache.
   * <p>
   * Always false for the "*bind" operations.
   * <p>
   * For "get", "getAll", and "list" the server is free to ignore
   * this flag and return a cached value.  This should only be
   * set to false if the client has detected a <b>strong</b>
   * out-of-band hint that the entry is stale, such as a lost
   * network connection to the entry's URI.
   * <p>
   * This field is ignored when comparing "equals()".
   */
  public final boolean useCache() {
    return useCache;
  }

  /**
   * The timeout indicates the maximum time in milliseconds
   * for the <i>resolver</i> action.
   * <p>
   * The valid timeout values are:</ul>
   *   <li>positive:<br>
   *       Maximum resolver time in milliseconds.  If the
   *       resolver takes longer than this time, it may
   *       continue the request in the background for future
   *       caching use.
   *   </li>
   *   <li>zero:<br>
   *       No timeout.
   *   </li>
   *   <li>negative:<br>
   *       Check the cache, but don't initiate a remote request
   *       if the entry is not in the cache.  This is only
   *       valid for the "get" and "list" operations.
   *   </li>
   * </ul>
   * <p>
   * Note that this can be different than the Response
   * "waitForIsAvailable(long timeout)", which is a timeout
   * for the response.  This is only useful if you have multiple
   * threads waiting for a response, e.g.<pre>
   *    - generate request with 10 minute timeout
   *    - thread A wants a response in 5 seconds
   *    - thread B is willing to wait forever (max = 10 minutes)
   * </pre>
   * <p>
   * This field is ignored when comparing "equals()".
   * <p>
   * @return milliseconds
   */
  public final long getTimeout() { 
    return timeout;
  }

  /**
   * Create a response object for this request.
   */
  public abstract Response createResponse();

  public String toString() {
    return 
      " oid="+System.identityHashCode(this)+
      " timeout="+getTimeout()+
      " useCache="+useCache()+
      ")";
  }

  /**
   * Get the entry with the matching (name, type) fields.
   *
   * @see Request.GetAll get all entries with a given name
   */
  public static class Get extends Request {
    private final String name;
    private final String type;
    private transient int _hc;
    public Get(
        boolean useCache, long timeout,
        String name, String type) {
      super(useCache, timeout);
      this.name = name;
      this.type = type;
      if (name==null || type==null) {
        throw new IllegalArgumentException("Null parameter");
      }
    }
    public final String getName() { return name; }
    public final String getType() { return type; }
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
        (useCache() ? "get" : "refresh")+
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
  public static class GetAll extends Request {
    private final String name;
    public GetAll(
        boolean useCache, long timeout,
        String name) {
      super(useCache, timeout);
      this.name = name;
      if (name == null) {
        throw new IllegalArgumentException("null name");
      }
    }
    public final String getName() { 
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
        (useCache() ? "getAll" : "refreshAll")+
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
  public static class List extends Request {
    private final String suffix;
    public List(
        boolean useCache, long timeout,
        String suffix) {
      super(useCache, timeout);
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
    public final String getSuffix() { 
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
        (useCache() ? "list" : "relist")+
        " suffix="+getSuffix()+
        super.toString();
    }
  }

  /**
   * Bind a new entry, or rebind an existing entry if the
   * overwrite flag is false.
   * <p>
   * The renewal flag is for the infrastructure's use, for
   * renewing bind leases.
   */
  public static class Bind extends Request {
    private final AddressEntry ae;
    private final boolean overWrite;
    private final boolean renewal;
    public Bind(
        boolean useCache,
        long timeout,
        AddressEntry ae,
        boolean overWrite,
        boolean renewal) {
      super(useCache, timeout);
      this.ae = ae;
      this.overWrite = overWrite;
      this.renewal = renewal;
      if (useCache) {
        throw new IllegalArgumentException(
            "Bind must have \"useCache\" set to false");
      }
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
      if (renewal && overWrite) {
        throw new IllegalArgumentException(
            "Renewal implies non-overwrite");
      }
    }
    public final AddressEntry getAddressEntry() { 
      return ae;
    }
    public final boolean isOverWrite() {
      return overWrite;
    }
    public final boolean isRenewal() {
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
         (overWrite ? 1 : 2)+
         (renewal ? 3 : 4));
    }
    public String toString() {
      return 
        "("+
        (isOverWrite() ?
         "rebind" :
         (isRenewal() ?
          "renew" : "bind"))+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }

  /**
   * Destroy the binding for the specified entry.
   * <p>
   * The client must pass the current value for the
   * bound entry.
   */
  public static class Unbind extends Request {
    private final AddressEntry ae;
    public Unbind(
        boolean useCache,
        long timeout,
        AddressEntry ae) {
      super(useCache, timeout);
      this.ae = ae;
      if (useCache) {
        throw new IllegalArgumentException(
            "Bind must have \"useCache\" set to false");
      }
      if (ae == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public final AddressEntry getAddressEntry() { return ae; }
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
        "(unbind"+
        " entry="+getAddressEntry()+
        super.toString();
    }
  }
}
