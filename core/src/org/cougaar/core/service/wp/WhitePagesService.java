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

import java.util.Set;
import org.cougaar.core.component.Service;

/**
 * The white pages service provides access to the distributed name
 * server.
 * <p>
 * The primary function of the white pages is to allow agents
 * to register their message transport addresses and lookup the
 * addresses of other agents.  This service is the client-side
 * resolver and may be backed by a cache.
 * <p>
 * The white pages service currently does not support a "listener"
 * API to watch for changes, primarily due to scalability concerns.
 */
public abstract class WhitePagesService implements Service {

  //
  // no-timeout variations:
  //

  /** @see Request.Get */
  public final AddressEntry[] get(String name) throws Exception {
    return get(name, 0);
  }
  /** @see #get(String,Application,String,long) */
  public final AddressEntry get(
      String name, Application app, String scheme) throws Exception {
    return get(name, app, scheme, 0);
  }
  /** @see Request.List */
  public final Set list(String suffix) throws Exception {
    return list(suffix, 0);
  }
  /** @see Request.Refresh */
  public final AddressEntry refresh(AddressEntry ae) throws Exception {
    return refresh(ae, 0);
  }
  /** @see Request.Bind */
  public final void bind(AddressEntry ae) throws Exception {
    bind(ae, 0);
  }
  /** @see Request.Rebind */
  public final void rebind(AddressEntry ae) throws Exception {
    rebind(ae, 0);
  }
  /** @see Request.Unbind */
  public final void unbind(AddressEntry ae) throws Exception {
    unbind(ae, 0);
  }

  //
  // timeout variations:
  //

  public static final class TimeoutException extends Exception {
    private final boolean b;
    public TimeoutException(boolean b) {
      super("Timeout on "+(b ? "Request" : "Response"));
      this.b = b;
    }
    /**
     * @return true if the Request timeout was too short, else
     * return false if the wait for the Response was too short.
     */
    public boolean isRequestTimeout() { return b; }
  }

  public final AddressEntry[] get(String name, long timeout) throws Exception {
    Request.Get req = new Request.Get(name, timeout);
    Response.Get res = (Response.Get) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return res.getAddressEntries();
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  /**
   * Utility method that calls "get(name, timeout)" and returns the
   * first entry with the specified application and scheme.
   */
  public final AddressEntry get(
      String name,
      Application app,
      String scheme,
      long timeout) throws Exception {
    if (name == null || app == null || scheme == null) {
      throw new IllegalArgumentException("Null parameter");
    }
    AddressEntry[] a = get(name, timeout);
    int n = (a == null ? 0 : a.length);
    for (int i = 0; i < n; i++) {
      AddressEntry ae = a[i];
      if (ae != null &&
          name.equals(ae.getName()) &&
          app.equals(ae.getApplication()) &&
          scheme.equals(ae.getAddress().getScheme())) {
        return ae;
      }
    }
    return null;
  }

  public final Set list(String suffix, long timeout) throws Exception {
    Request.List req = new Request.List(suffix, timeout);
    Response.List res = (Response.List) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return res.getNames();
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  public final AddressEntry refresh(AddressEntry ae, long timeout) throws Exception {
    Request.Refresh req = new Request.Refresh(ae, timeout);
    Response.Refresh res = (Response.Refresh) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return res.getNewEntry();
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  public final void bind(AddressEntry ae, long timeout) throws Exception {
    Request.Bind req = new Request.Bind(ae, timeout);
    Response.Bind res = (Response.Bind) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return;
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  public final void rebind(AddressEntry ae, long timeout) throws Exception {
    Request.Rebind req = new Request.Rebind(ae, timeout);
    Response.Rebind res = (Response.Rebind) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return;
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  public final void unbind(AddressEntry ae, long timeout) throws Exception {
    Request.Unbind req = new Request.Unbind(ae, timeout);
    Response.Unbind res = (Response.Unbind) submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return;
      } else if (res.isTimeout()) {
        throw new TimeoutException(true);
      } else {
        throw res.getException();
      }
    } else {
      throw new TimeoutException(false);
    }
  }

  /**
   * Submit a request, get back a "future reply" response.
   * <p>
   * Example blocking usage is illustrated in the above
   * utility methods, such as:<pre>
   *   try {
   *     AddressEntry[] a = wps.get("foo");
   *     System.out.println("got foo: "+a);
   *   } catch (Exception e) {
   *     System.out.println("failed: "+e);
   *   }
   * </pre>
   * <p>
   * An example "polling" usage is:<pre>
   *    Request req = new Request.Get("foo");
   *    Response res = wps.submit(req);
   *    while (!res.isAvailable()) {
   *      try {
   *        Thread.sleep(1000);       // wait a sec
   *      } catch (InterruptedException ie) {}
   *    }
   *    Response.Get getRes = (Response.Get) res;
   *    System.out.println(getRes);
   * </pre>
   * <p>
   * An example "callback" usage is:<pre>
   *    Response r = yps.submitQuery(q);
   *    Callback callback = new Callback() {
   *      public void execute(Response res) {
   *        Response.Get getRes = (Response.Get) res;
   *        System.out.println(getRes);
   *      }
   *    }
   *    // add callback, will callback immediately if 
   *    // there is already an answer
   *    r.addCallback(callback);
   *    // keep going, or optionally wait for the callback
   * </pre>
   * <p>
   * One can also mix the polling and callback styles:<pre>
   *    Request.Get req = new Request.Get("foo");
   *    Response.Get res = (Response.Get) wps.submit(req);
   *    if (res.waitForIsAvailable(5000)) {
   *      System.out.println("Got in 5 secs: "+res);
   *    } else {
   *      Callback callback = new Callback() {
   *        public void execute(Response r) {
   *          assert r == res;
   *          System.out.println("Got later: "+r);
   *        }
   *      }
   *      res.addCallback(callback);
   *    }
   * </pre>
   *
   * @param request the non-null request
   * @return a non-null response
   */
  public abstract Response submit(Request req);

  /**
   * Submit with a callback.
   * <p>
   * Equivalent to:<pre>
   *    Response res = submit(req);
   *    res.addCallback(c);
   *    return res;
   * </pre>
   */
  public final Response submit(Request req, Callback c) {
    Response res = submit(req);
    res.addCallback(c);
    return res;
  }

}
