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
 */
public abstract class Request implements Serializable {

  private final long timeout;

  private Request() {
    this(0);
  }
  private Request(long timeout) {
    this.timeout = timeout;
  }

  /**
   * The timeout indicates the maximum time in milliseconds
   * for the <i>resolver</i> action, where zero indicates
   * "forever".
   * <p>
   * Note that this can be different than the Response
   * "waitForIsAvailable(long timeout)", which is a timeout
   * for the response.  This is only useful if you have multiple
   * threads waiting for a response, e.g.<pre>
   *    - generate request with 10 minute timeout
   *    - thread A wants a response in 5 seconds
   *    - thread B is willing to wait forever (max = 10 minutes)
   * </pre>
   *
   * @return millis
   */
  public final long getTimeout() { 
    return timeout;
  }

  public abstract Response createResponse();

  public String toString() {
    return 
      " oid="+System.identityHashCode(this)+
      ((timeout > 0) ? (" timeout="+timeout) : "")+
      ")";
  }

  /**
   * Get all entries associated with the given name.
   *
   * @see Get.Refresh do a specific (name, app, scheme) lookup,
   *   or update an existing entry.
   */
  public static class Get extends Request {
    private final String name;
    public Get(String name) {
      this(name, 0);
    }
    public Get(String name, long timeout) {
      super(timeout);
      this.name = name;
      if (name == null) {
        throw new IllegalArgumentException("null name");
      }
    }
    public final String getName() { 
      return name;
    }
    public Response createResponse() {
      return new Response.Get(this);
    }
    public String toString() {
      return "(get name="+getName()+super.toString();
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
   * This is somewhat like a the DNS zone transfer (AXFR) limited to
   * depth=1.
   */
  public static class List extends Request {
    private final String suffix;
    public List(String suffix) {
      this(suffix, 0);
    }
    public List(String suffix, long timeout) {
      super(timeout);
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
    public String toString() {
      return "(list suffix="+getSuffix()+super.toString();
    }
  }

  /**
   * Refresh the specified entry.
   * <p>
   * Only these fields of the entry are used:
   * <ul>
   *   <li>entry.getName()  <i>e.g. "foo"</i></li>
   *   <li>entry.getApplication()   <i>e.g. "wp"</i></li>
   *   <li>entry.getAddress().getScheme()  <i>e.g. "rmi"</i></li>
   * </ul><br>
   * Note that this allows a client to do a simple "get" for just
   * one (name, application, scheme) tuple.
   * <p>
   * If the client already has an entry, one can't necessarily
   * use the TTL to guarantee validity.  The entry TTL is primarily
   * for the resolver's cache use.  A rebind may update the entry 
   * before the TTL has expired.  Calling "refresh" will force both
   * a cache and entry update, and the returned entry is guaranteed
   * to either be null or have a TTL in the future.
   * <p>
   * This is typically only used if the client has detected a
   * <b>strong</b> out-of-band hint that the entry is stale, such
   * as a lost network connection to the entry's URI.
   */
  public static class Refresh extends Request {
    private final AddressEntry oa;
    public Refresh(AddressEntry oa) {
      this(oa, 0);
    }
    public Refresh(AddressEntry oa, long timeout) {
      super(timeout);
      this.oa = oa;
      if (oa == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public final AddressEntry getOldEntry() { 
      return oa;
    }
    public Response createResponse() {
      return new Response.Refresh(this);
    }
    public String toString() {
      return "(refresh old="+getOldEntry()+super.toString();
    }
  }

  /**
   * Bind a new entry.
   * <p>
   * The entry TTL must be in the future.
   */
  public static class Bind extends Request {
    private final AddressEntry a;
    public Bind(AddressEntry a) {
      this(a, 0);
    }
    public Bind(AddressEntry a, long timeout) {
      super(timeout);
      this.a = a;
      if (a == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public final AddressEntry getAddressEntry() { 
      return a;
    }
    public Response createResponse() {
      return new Response.Bind(this);
    }
    public String toString() {
      return "(bind entry="+getAddressEntry()+super.toString();
    }
  }

  /**
   * Rebinds the specified entry with the new value, where any
   * existing binding for the name is replaced.
   * <p>
   * The entry TTL must be in the future.
   */
  public static class Rebind extends Request {
    private final AddressEntry a;
    public Rebind(AddressEntry a) {
      this(a, 0);
    }
    public Rebind(AddressEntry a, long timeout) {
      super(timeout);
      this.a = a;
      if (a == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public final AddressEntry getAddressEntry() { 
      return a;
    }
    public Response createResponse() {
      return new Response.Rebind(this);
    }
    public String toString() {
      return "(rebind entry="+getAddressEntry()+super.toString();
    }
  }

  /**
   * Destroy the binding for the specified entry.
   * <p>
   * Only these fields of the entry are used:
   * <ul>
   *   <li>entry.getName()  <i>e.g. "foo"</i></li>
   *   <li>entry.getApplication()   <i>e.g. "wp"</i></li>
   *   <li>entry.getAddress().getScheme()  <i>e.g. "rmi"</i></li>
   * </ul><br>
   * This allows a client to remove an entry without worrying
   * about the current value (if any).
   */
  public static class Unbind extends Request {
    private final AddressEntry a;
    public Unbind(AddressEntry a) {
      this(a, 0);
    }
    public Unbind(AddressEntry a, long timeout) {
      super(timeout);
      this.a = a;
      if (a == null) {
        throw new IllegalArgumentException("Null entry");
      }
    }
    public final AddressEntry getAddressEntry() { 
      return a;
    }
    public Response createResponse() {
      return new Response.Unbind(this);
    }
    public String toString() {
      return "(unbind entry="+getAddressEntry()+super.toString();
    }
  }
}
