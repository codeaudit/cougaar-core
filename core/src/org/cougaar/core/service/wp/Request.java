/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
   * Get all entries for all names at this level in the
   * naming hierarchy.
   * <p>
   * This presents scalability concerns, so use with caution.
   */
  public static class GetAll extends Request {
    public GetAll() {
      this(0);
    }
    public GetAll(long timeout) {
      super(timeout);
    }
    public Response createResponse() {
      return new Response.GetAll(this);
    }
    public String toString() {
      return "(getAll"+super.toString();
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
   * strong out-of-band hint that the entry is stale, such as a
   * lost network connection to the entry's address.
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
