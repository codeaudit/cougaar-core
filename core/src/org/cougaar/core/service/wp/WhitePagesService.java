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

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.component.Service;
import org.cougaar.util.log.Logging;
import org.cougaar.util.log.Logger;

/**
 * The white pages service provides access to the distributed name
 * server.
 * <p>
 * The primary function of the white pages is to allow agents
 * to register their message transport addresses and lookup the
 * addresses of other agents.  This service is the client-side
 * resolver and is backed by a cache.
 * <p>
 * The main method of this class is:<pre>
 *    public abstract Response submit(Request req);
 * </pre>
 * This submits an request with an asynchronous response.  The
 * caller can attach callbacks to the response or block until
 * the response result is set.  All the other methods of the
 * WhitePagesService are based upon the above "submit" method.
 * <p>
 * The white pages service currently does not support a "listener"
 * API to watch for changes, primarily due to scalability concerns.
 */
public abstract class WhitePagesService implements Service {

  //
  // no-timeout variations:
  //

  /** @see Request.Get */
  public final AddressEntry get(
      String name, String type) throws Exception {
    return get(name, type, 0);
  }
  /** @see Request.GetAll */
  public final Map getAll(String name) throws Exception {
    return getAll(name, 0);
  }
  /** @see Request.List */
  public final Set list(String suffix) throws Exception {
    return list(suffix, 0);
  }
  /** @see Request.Get */
  public final AddressEntry refresh(
      String name, String type) throws Exception {
    return refresh(name, type, 0);
  }
  /** @see Request.Bind */
  public final void bind(AddressEntry ae) throws Exception {
    bind(ae, 0);
  }
  /** @see Request.Bind */
  public final void rebind(AddressEntry ae) throws Exception {
    rebind(ae, 0);
  }
  /** @see Request.Unbind */
  public final void unbind(AddressEntry ae) throws Exception {
    unbind(ae, 0);
  }

  // 
  // callback variations:
  //

  /** @see Request.Get */
  public final void get(
      String name, String type, Callback callback) throws Exception {
    submit(new Request.Get(true, 0, name, type), callback);
  }
  /** @see Request.GetAll */
  public final void getAll(String name, Callback callback) throws Exception {
    submit(new Request.GetAll(true, 0, name), callback);
  }
  /** @see Request.List */
  public final void list(String suffix, Callback callback) throws Exception {
    submit(new Request.List(true, 0, suffix), callback);
  }
  /** @see Request.Get */
  public final void refresh(
      String name, String type, Callback callback) throws Exception {
    submit(new Request.Get(false, 0, name, type), callback);
  }
  /** @see Request.Bind */
  public final void bind(AddressEntry ae, Callback callback) throws Exception {
    submit(new Request.Bind(false, 0, ae, false, false), callback);
  }
  /** @see Request.Bind */
  public final void rebind(AddressEntry ae, Callback callback) throws Exception {
    submit(new Request.Bind(false, 0, ae, true, false), callback);
  }
  /** @see Request.Rebind */
  public final void unbind(AddressEntry ae, Callback callback) throws Exception {
    submit(new Request.Unbind(false, 0, ae), callback);
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

  /** @see Request.Get */
  public final AddressEntry get(
      String name, String type,
      long timeout) throws Exception {
    Request.Get req = 
      new Request.Get(true, timeout, name, type);
    Response.Get res = (Response.Get) assertSubmit(req);
    return res.getAddressEntry();
  }
  /** @see Request.GetAll */
  public final Map getAll(String name, long timeout) throws Exception {
    Request.GetAll req = new Request.GetAll(true, timeout, name);
    Response.GetAll res = (Response.GetAll) assertSubmit(req);
    return res.getAddressEntries();
  }
  /** @see Request.List */
  public final Set list(String suffix, long timeout) throws Exception {
    Request.List req = new Request.List(true, timeout, suffix);
    Response.List res = (Response.List) assertSubmit(req);
    return res.getNames();
  }
  /** @see Request.Get */
  public final AddressEntry refresh(
      String name, String type,
      long timeout) throws Exception {
    Request.Get req = new Request.Get(
        false, timeout, name, type);
    Response.Get res = (Response.Get) assertSubmit(req);
    return res.getAddressEntry();
  }
  /** @see Request.Bind */
  public final void bind(AddressEntry ae, long timeout) throws Exception {
    Request.Bind req = new Request.Bind(false, timeout, ae, false, false);
    Response.Bind res = (Response.Bind) assertSubmit(req);
    if (!res.didBind()) {
      throw new RuntimeException("Bind failed: "+res);
    }
  }
  /** @see Request.Bind */
  public final void rebind(AddressEntry ae, long timeout) throws Exception {
    Request.Bind req = new Request.Bind(false, timeout, ae, true, false);
    Response.Bind res = (Response.Bind) assertSubmit(req);
    if (!res.didBind()) {
      throw new RuntimeException("Rebind failed: "+res);
    }
  }
  /** @see Request.Unbind */
  public final void unbind(AddressEntry ae, long timeout) throws Exception {
    Request.Unbind req = new Request.Unbind(false, timeout, ae);
    Response.Unbind res = (Response.Unbind) assertSubmit(req);
  }

  /**
   * Submit a request and return the response if it is completed
   * within the request's timeout and successful, otherwise throw
   * an exception.
   */
  public final Response assertSubmit(Request req) throws Exception {
    long timeout = req.getTimeout();
    Response res = submit(req);
    if (res.waitForIsAvailable(timeout)) {
      if (res.isSuccess()) {
        return res;
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

  /**
   * Submit a request, get back a "future reply" response.
   * <p>
   * Example blocking usage is illustrated in the above
   * utility methods, such as:<pre>
   *   try {
   *     Map m = wps.getAll("foo");
   *     System.out.println("got all entries for foo: "+m);
   *   } catch (Exception e) {
   *     System.out.println("failed: "+e);
   *   }
   * </pre>
   * <p>
   * An example timed block:<pre>
   *   try {
   *     // wait at most 5 seconds
   *     Map m = wps.getAll("foo", 5000);
   *     System.out.println("got all entries for foo: "+m);
   *   } catch (WhitePagesService.TimeoutException te) {
   *     System.out.println("timeout"); // waited over 5 seconds
   *   } catch (Exception e) {
   *     System.out.println("failed: "+e);
   *   }
   * </pre>
   * <p>
   * An example "callback" usage is:<pre>
   *    Request req = new Request.GetAll("foo");
   *    Response r = wps.submit(req);
   *    Callback callback = new Callback() {
   *      public void execute(Response res) {
   *        // note that (res == r)
   *        if (res.isSuccess()) {
   *          Map m = ((Response.GetAll) res).getAddressEntries();
   *          System.out.println("got all entries for foo: "+m);
   *        } else {
   *          System.out.println("failed: "+res);
   *        }
   *      }
   *    };
   *    // add callback, will execute immediately if 
   *    // there is already an answer
   *    r.addCallback(callback);
   *    // keep going, or optionally wait for the callback
   * </pre>
   * <p>
   * An example blocking "poll" usage is:<pre>
   *    Request req = new Request.GetAll("foo");
   *    Response res = wps.submit(req);
   *    // wait at most 5 seconds
   *    if (res.waitForIsAvailable(5000)) {
   *      // got an answer in under 5 seconds
   *      if (res.isSuccess()) {
   *        Map m = ((Response.GetAll) res).getAddressEntries();
   *        System.out.println("got all entries for foo: "+m);
   *      } else {
   *        System.out.println("failed: "+res);
   *      }
   *    } else {
   *      // can wait some more, attach a callback, or give up
   *    }
   * </pre>
   *
   * @param request the non-null request
   * @return a non-null response
   */
  public abstract Response submit(Request req);

  //
  // deprecated, to be removed in Cougaar 10.4.1+
  //

  /** @deprecated use "Map getAll(name)" */
  public final AddressEntry[] get(String name) throws Exception {
    return get(name, 0);
  }
  /** @deprecated use "get(name,type)" */
  public final AddressEntry get(
      String name, Application app, String scheme) throws Exception {
    return get(name, app, scheme, 0);
  }
  /** @deprecated use "refresh(name,type)" */
  public final AddressEntry refresh(AddressEntry ae) throws Exception {
    return refresh(ae, 0);
  }
  /** @deprecated use "Map getAll(name,timeout)" */
  public final AddressEntry[] get(String name, long timeout) throws Exception {
    Map m = getAll(name, 0);
    AddressEntry[] ret;
    if (m == null || m.isEmpty()) {
      ret = new AddressEntry[0];
    } else {
      int msize = m.size();
      ret = new AddressEntry[msize];
      int i = 0;
      for (Iterator iter = m.values().iterator();
          iter.hasNext();
          ) {
        AddressEntry aei = (AddressEntry) iter.next();
        ret[i++] = aei;
      }
    }
    return ret;
  }
  /** @deprecated use "get(name,type,timeout)" */
  public final AddressEntry get(
      String name,
      Application app,
      String scheme,
      long timeout) throws Exception {
    String origType = app.toString();
    String type = origType;
    // backwards compatibility hack:
    if ("topology".equals(type) && "version".equals(scheme)) {
      type="version";
    }
    if (type != origType) {
      Exception e = 
        new RuntimeException(
            "White pages \"get("+name+
            ", "+origType+", "+scheme+
            ")\" should be replaced with"+
            "\"get("+name+", "+type+")\"");
      Logging.getLogger(getClass()).warn(
          null, e);
    }
    AddressEntry ae = get(name, type, timeout);
    if (ae != null &&
        !scheme.equals(ae.getURI().getScheme())) {
      Exception e = 
        new RuntimeException(
            "White pages \"get("+name+
            ", "+origType+", "+scheme+
            ")\" returned an entry with a different"+
            " URI scheme: "+ae);
      Logging.getLogger(getClass()).warn(
          null, e);
    }
    return ae;
  }
  /** @deprecated use "refresh(name,type,timeout)" */
  public final AddressEntry refresh(
      AddressEntry ae, long timeout) throws Exception {
    return refresh(ae.getName(), ae.getType(), timeout);
  }
}
