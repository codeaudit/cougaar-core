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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Response from the white pages service.
 */
public abstract class Response implements Serializable {

  /** A marker exception for a request timeout failure */
  public static final String TIMEOUT = "timeout";

  private final Request request;
  private final Object lock = new Object();
  private Set callbacks;
  private Object result;

  private Response(Request request) {
    this.request = request;
    if (request == null) {
      throw new RuntimeException("Null request");
    }
  }

  public final Request getRequest() { 
    return request;
  }

  public boolean isAvailable() {
    return (getResult() != null);
  }

  /** Suspend the current thread until response.isAvailable() will return true
   * @return true
   */
  public boolean waitForIsAvailable() throws InterruptedException {
    return waitForIsAvailable(0);
  }

  /** Suspend the current thread until response.isAvailable() will return true
   * @param timeout How long to wait.
   * @return true if "isAvailable()"
   **/
  public boolean waitForIsAvailable(long timeout) throws InterruptedException {
    synchronized (lock) {
      if (result != null) {
        return true;
      }
      lock.wait(timeout);
      return (result != null);
    }
  }

  /** install a callback to be invoked when the response is available.
   * If the response is already available when this method is called,
   * the callback my be invoked in the calling thread immediately.
   * @note The behavior of this method is undefined if it is called more than
   * once on a single YPResponse instance.
   * @param callback A runnable to be executed when a result is available.  This will
   * be called exactly once.  The callback.execute(Result) method should execute
   * quickly - under no circumstances should it ever block or perform any
   * non-trivial tasks.
   **/
  public void addCallback(Callback c) {
    if (c == null) {
      throw new IllegalArgumentException("Null callback");
    }
    synchronized (lock) {
      if (result != null) {
        c.execute(this);
      } else {
        if (callbacks == null) {
          callbacks = Collections.singleton(c);
        } else if (!callbacks.contains(c)) {
          if (callbacks.size() == 1) {
            Object o = callbacks.iterator().next();
            callbacks = new HashSet(5);
            callbacks.add(o);
          }
          callbacks.add(c);
        }
      }
    }
  }

  /**
   * Remove a callback.
   */
  public void removeCallback(Callback c) {
    if (c == null) {
      throw new IllegalArgumentException("Null callback");
    }
    synchronized (lock) {
      if (result == null && 
          callbacks != null) {
        if (callbacks.size() == 1) {
          if (callbacks.contains(c)) {
            callbacks = null;
          }
        } else {
          callbacks.remove(c);
        }
      }
      // return true if was contained?  not safe, since
      // callbacks are invoked outside the lock...
    }
  }

  public final Object getResult() { 
    synchronized (lock) {
      return result;
    }
  }

  public final void setResult(Object r) { 
    if (r == null) {
      throw new IllegalArgumentException("Null result");
    }
    Set s;
    synchronized (lock) {
      if (result != null) {
        if (r == TIMEOUT || result == TIMEOUT) {
          // okay, ignored timeout
          return;
        }
        throw new RuntimeException(
            "Result already set to "+result+
            ", won't replace with "+r);
      }
      this.result = r;
      lock.notifyAll();
      if (callbacks == null) {
        return;
      }
      s = callbacks;
      callbacks = null;
    }
    for (Iterator iter = s.iterator(); iter.hasNext(); ) {
      Callback c = (Callback) iter.next();
      c.execute(this);
    }
  }

  public String toString() {
    synchronized (lock) {
      StringBuffer buf = new StringBuffer();
      buf.append("(response oid=");
      buf.append(System.identityHashCode(this));
      buf.append(" req=").append(request);
      if (result != null) {
        buf.append(" val=");
        if (result instanceof Object[]) {
          Object[] a = (Object[]) result;
          int n = a.length;
          buf.append("[").append(n).append("]{");
          if (n > 0) {
            while (true) {
              buf.append(a[--n]);
              if (n <= 0) break;
              buf.append(", ");
            }
          }
          buf.append("}");
        } else {
          buf.append(result);
        }
      }
      buf.append(")");
      return buf.toString();
    }
  }

  public boolean isSuccess() {
    Object r = getResult();
    return 
      (!
       (r == null ||
        r == TIMEOUT ||
        r instanceof Exception));
  }

  public boolean isTimeout() {
    Object r = getResult();
    return (r == TIMEOUT);
  }

  public Exception getException() {
    Object r = getResult();
    return 
      (r instanceof Exception) ?
      ((Exception) r) :
      null;
  }

  /**
   * Get all entries associated with the given name.
   */
  public static class Get extends Response {
    public Get(Request q) {
      this((Request.Get) q);
    }
    public Get(Request.Get q) {
      super(q);
    }
    public AddressEntry[] getAddressEntries() { 
      Object r = getResult();
      return
        (r instanceof AddressEntry[]) ?
        ((AddressEntry[]) r) :
        null;
    }
  }

  /**
   * List response.
   */
  public static class List extends Response {
    public List(Request q) {
      this((Request.List) q);
    }
    public List(Request.List q) {
      super(q);
    }
    public Set getNames() { 
      Object r = getResult();
      return
        (r instanceof Set) ?
        ((Set) r) :
        null;
    }
  }

  /**
   * Refresh the specified entry.
   * <p>
   * Note that an entry TTL indicates the resolver's cache lifetime,
   * not necessarily the validity of the entry.  A rebind may update
   * the entry before the TTL has expired.  Calling "refresh" will
   * force both a cache and entry update.
   * <p>
   * This is typically only used if the client has detected a
   * strong out-of-band hint that the entry is stale, such as a
   * lost network connection to the entry's address.
   */
  public static class Refresh extends Response {
    public static final Object NULL = new Object();
    public Refresh(Request q) {
      this((Request.Refresh) q);
    }
    public Refresh(Request.Refresh q) {
      super(q);
    }
    public AddressEntry getNewEntry() { 
      Object r = getResult();
      return
        (r instanceof AddressEntry) ?
        ((AddressEntry) r) :
        null;
    }
  }

  /**
   * Bind a new entry.
   */
  public static class Bind extends Response {
    public Bind(Request q) {
      this((Request.Bind) q);
    }
    public Bind(Request.Bind q) {
      super(q);
    }
  }

  /**
   * Rebinds the specified entry with the new value, where any
   * existing binding for the name is replaced.
   */
  public static class Rebind extends Response {
    public Rebind(Request q) {
      this((Request.Rebind) q);
    }
    public Rebind(Request.Rebind q) {
      super(q);
    }
  }

  /**
   * Destroy the binding for the specified entry.
   */
  public static class Unbind extends Response {
    public Unbind(Request q) {
      this((Request.Unbind) q);
    }
    public Unbind(Request.Unbind q) {
      super(q);
    }
  }
}
