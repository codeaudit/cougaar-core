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
import java.util.Map;
import java.util.Set;
import org.cougaar.core.thread.SchedulableStatus;

/**
 * Response from the white pages service.
 */
public abstract class Response implements Callback, Serializable {

  /** A marker exception for a request timeout failure */
  public static final String TIMEOUT = "timeout";

  private final Request request;
  private final Object lock = new Object();
  private transient Set callbacks;
  private Object result;

  /**
   * Responses are created by asking a Request to
   * "createResponse()".
   */
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

  /**
   * Suspend the current thread until response.isAvailable()
   * will return true
   * <p>
   * @return true
   */
  public boolean waitForIsAvailable() throws InterruptedException {
    return waitForIsAvailable(0);
  }

  /**
   * Suspend the current thread until response.isAvailable() will
   * return true or the timeout is exceeded.
   * <p>
   * @param timeout How long to wait.
   * @return true if "isAvailable()"
   */
  public boolean waitForIsAvailable(long timeout) throws InterruptedException {
    synchronized (lock) {
      if (result != null) {
        return true;
      }
      try {
	SchedulableStatus.beginWait("WP lookup");
        lock.wait(timeout);
      } finally {
	SchedulableStatus.endBlocking();
      }
      return (result != null);
    }
  }

  /** 
   * Install a callback to be invoked when the response is available.
   * <p>
   * If the response is already available when this method is called,
   * the callback my be invoked in the calling thread immediately.
   * <p>
   * @param callback A runnable to be executed when a result is
   * available.  This will be called exactly once.  The
   * callback.execute(Result) method should execute quickly - under
   * no circumstances should it ever block or perform any non-trivial
   * tasks.
   */
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

  public final boolean setResult(Object r) {
    if (r == null) {
      r = getDefaultResult();
    }
    Set s;
    synchronized (lock) {
      if (result != null) {
        if (r == TIMEOUT || result == TIMEOUT) {
          // okay, ignored timeout
          return true;
        }
        return false;
      }
      this.result = r;
      lock.notifyAll();
      if (callbacks == null) {
        return true;
      }
      s = callbacks;
      callbacks = null;
    }
    for (Iterator iter = s.iterator(); iter.hasNext(); ) {
      Callback c = (Callback) iter.next();
      c.execute(this);
    }
    return true;
  }

  protected abstract Object getDefaultResult();

  // let a response be a callback, for easy chaining
  public void execute(Response res) {
    if (res == this) {
      // invalid chain!
      return;
    }
    if (res == null || !res.isAvailable()) {
      throw new IllegalArgumentException(
          "Invalid callbach result: "+res);
    }
    setResult(res.getResult());
  }

  // equals is ==

  public String toString() {
    Object r = getResult();
    return
      "(response oid="+
      System.identityHashCode(this)+
      " req="+request+
      (r == null ? "" : " result="+r)+
      ")";
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

  /** @see Request.Get */
  public static class Get extends Response {
    public static final Object NULL = new Object() {
      private Object readResolve() { return NULL; }
      public String toString() { return "null_get"; }
    };
    public Get(Request q) {
      this((Request.Get) q);
    }
    public Get(Request.Get q) {
      super(q);
    }
    public AddressEntry getAddressEntry() { 
      Object r = getResult();
      if (r instanceof AddressEntry) {
        return ((AddressEntry) r);
      } else if (r == NULL) {
        return null;
      } else if (r instanceof Map) {
        // the server can answer "get" with "getAll" map
        Map m = (Map) r;
        Request.Get rg = (Request.Get) getRequest();
        String n = rg.getName();
        String type = rg.getType();
        return (AddressEntry) m.get(type);
      } else {
        // !isSuccess
        return null;
      }
    }
    protected Object getDefaultResult() {
      return NULL;
    }
  }

  /** @see Request.GetAll */
  public static class GetAll extends Response {
    public GetAll(Request q) {
      this((Request.GetAll) q);
    }
    public GetAll(Request.GetAll q) {
      super(q);
    }
    /**
     * @return a Map of (String type, AddressEntry entry) pairs,
     *   where the type matches the `entry.getType()`
     */
    // Map<String,AddressEntry>
    public Map getAddressEntries() { 
      Object r = getResult();
      return
        (r instanceof Map) ?
        ((Map) r) :
        null;
    }
    protected Object getDefaultResult() {
      return Collections.EMPTY_MAP;
    }
  }

  /** @see Request.List */
  public static class List extends Response {
    public List(Request q) {
      this((Request.List) q);
    }
    public List(Request.List q) {
      super(q);
    }
    /**
     * @return a Set of entry names.
     */
    // Set<String>
    public Set getNames() { 
      Object r = getResult();
      return
        (r instanceof Set) ?
        ((Set) r) :
        null;
    }
    protected Object getDefaultResult() {
      return Collections.EMPTY_SET;
    }
  }

  /** @see Request.Flush */
  public static class Flush extends Response {
    public Flush(Request q) {
      this((Request.Flush) q);
    }
    public Flush(Request.Flush q) {
      super(q);
    }
    /** Did the flush modify the local cache or force a lookup? */
    public boolean modifiedCache() {
      Object r = getResult();
      return
        (r instanceof Boolean) ?
        ((Boolean) r).booleanValue() :
        false;
    }
    protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }

  /** @see Request.Bind */
  public static class Bind extends Response {
    public Bind(Request q) {
      this((Request.Bind) q);
    }
    public Bind(Request.Bind q) {
      super(q);
    }
    /** Was the bind successful? */
    public boolean didBind() {
      return (getExpirationTime() > 0);
    }
    /** 
     * If <code>(didBind() == false)<code>, was the failure
     * due to another conflicting local bind while this bind
     * request was still pending?
     * <p>
     * Other possibilities include a bind-usurper
     * (<code>getUsurperEntry()</code>) or an exception
     * (<code>getException()</code>).
     */
    public boolean wasCanceled() {
      return (getCancelingRequest() != null);
    }
    /**
     * If successfully bound or renewed, when does the lease
     * expire?
     */
    public long getExpirationTime() {
      Object r = getResult();
      return
        (r instanceof Long) ?
        ((Long) r).longValue() :
        -1;
    }
    /**
     * If not bound or renewed, who took our place?
     * <p>
     * This is always null for a rebind (overwrite == true).
     * <p>
     * Isn't this !isSuccess ?
     */
    public AddressEntry getUsurperEntry() {
      Object r = getResult();
      return
        (r instanceof AddressEntry) ?
        ((AddressEntry) r) :
        null;
    }
    /**
     * If this request was canceled by another conflicting
     * local bind, what was the request?
     *
     * @return a Request.Bind or Request.Unbind
     */
    public Request getCancelingRequest() {
      Object r = getResult();
      return
        (r instanceof Request) ?
        ((Request) r) :
        null;
    }
    protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }

  /** @see Request.Unbind */
  public static class Unbind extends Response {
    public Unbind(Request q) {
      this((Request.Unbind) q);
    }
    public Unbind(Request.Unbind q) {
      super(q);
    }
    /**
     * Did the unbind succeed?
     * <p>
     * isn't this the same as isSuccess ?
     */
    public final boolean didUnbind() {
      Object r = getResult();
      return Boolean.TRUE.equals(r);
    }
    protected Object getDefaultResult() {
      return Boolean.FALSE;
    }
  }
}
