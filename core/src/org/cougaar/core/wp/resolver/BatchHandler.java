/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.wp.Timestamp;

/**
 * The batch aggregates requests by sending the first request
 * and batching subsequent identical requests until the
 * first request has been answered.
 * <p>
 * This class also upgrades all "get" requests to "getAll" requests,
 * since they are easier to batch and (maybe) reduce the server load.
 */
public class BatchHandler
extends HandlerBase
{

  private final BatchTable table = new BatchTable();

  protected Response mySubmit(Response res) {
    Request req = res.getRequest();
    if (req instanceof Request.Get) {
      Request.Get greq = (Request.Get) req;
      // work around some known bootstrapping bugs
      if (workaroundGetBugs(greq, res)) {
        return res;
      }
      // upgrade "get" to "getAll"
      Request sendReq = new Request.GetAll(
          greq.useCache(),
          greq.getTimeout(),
          greq.getName());
      Response sendRes = sendReq.createResponse();
      // chain to client's res
      sendRes.addCallback(res);
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Chain get->getAll submit="+res+" to res="+sendRes);
      }
      res = sendRes;
    }
    // add to the batch table
    if (enqueue(res)) {
      // either set or already batch
      if (res.isAvailable()) {
        return res;
      } else {
        return null;
      }
    }
    return res;
  }

  protected void myExecute(
      Request req, Object result, long ttl) {
    setResult(req, result, ttl);
  }

  // * @return true if already on the queue
  private boolean enqueue(Response res) {
    Request req = res.getRequest();
    // set timeout
    long timeout = req.getTimeout();
    if (timeout < 0) {
      // no remote lookup requested
      res.setResult(null, -1);
      return true;
    }
    if (timeout > 0 &&
        timeout < Long.MAX_VALUE) {
      TimeoutHandler th = new TimeoutHandler(res);
      threadService.schedule(th, timeout);
      res.addCallback(th);
    }
    // add to batch table
    boolean alreadyBatched = table.add(req, res);
    return alreadyBatched;
  }

  private void setResult(
      Request req, Object result, long ttl) {
    table.setResult(req, result, ttl);
  }

  private void timeout(Response res) {
    // slight race here, but the res.setResult
    // will filter out a late timeout
    if (res.isAvailable()) {
      // cancel race was late,
      // non-timeout result already set
    } else if (!removeResponse(res)) {
      // another harmless race
    } else {
      // set result in a separate thread
      res.setResult(Response.TIMEOUT, -1);
    }
  }

  // remove the specified response from the
  // batch table queue, return true if it was listed.
  //
  // this is used by the timeout callback
  private boolean removeResponse(Response res) {
    boolean wasListed = table.remove(res);
    return wasListed;
  }

  /**
   * Handle some known bootstrapping bugs.
   * <p>
   * Bug 2837:<br>
   * The MTS blocks on the version lookup in SendLinkImpl's
   * constructor, during the MTS load.  The fix is to move the
   * version info out of the WP.
   * <p>
   * Bug 2838:<br>
   * The MTS blocks on the link "cost" selection for local agents,
   * since * the RMI/CORBA/etc links must resolve their WP entry to
   * estimate their cost.  The fix is to stop the cost comparison
   * when loopback returns zero cost.  This may still break if
   * loopback is disabled, and would require a change to the
   * WP semantics:<pre>
   *    If a "bind" is in progress, should a local "get" show the
   *    unacknowledged bind or should it wait until the server
   *    acknowledges the bind?
   * </pre>
   * <p>
   * check for a pending bind and pretend that it went
   * through.
   * 
   * @return true if the workaround was applied and set the result.
   */
  private boolean workaroundGetBugs(
      Request.Get greq,
      Response res) {
    String gname = greq.getName();
    String gtype = greq.getType();
    if (gname.endsWith("(MTS)") &&
        "version".equals(gtype)) {
      // bug 2837
      if (logger.isInfoEnabled()) {
        logger.info(
            "Cancelling version lookup "+greq+
            " for the mts-internal name \""+gname+
            "\" (bug 2837)");
      }
      res.setResult(null, 1000);
      return true;
    }
    AddressEntry pendingAE = table.findBind(gname, gtype);
    if (pendingAE != null) {
      if (gtype.equals("version")) {
        // bug 2837
        if (logger.isInfoEnabled()) {
          logger.info(
              "Short-cutting "+gtype+" lookup "+greq+
              " for the pending bind "+pendingAE+
              " (bug 2837)");
        }
      } else if (gname.equals(agentId.getAddress())) {
        // bug 2839
        if (logger.isInfoEnabled()) {
          logger.info(
              "Short-cutting "+gtype+" lookup "+greq+
              " for the pending bind "+pendingAE+
              " (bug 2839)");
        }
      } else {
        if (logger.isWarnEnabled()) {
          logger.warn(
              "Short-cutting "+gtype+" lookup "+greq+
              " for the pending bind "+pendingAE);
        }
      }
      res.setResult(pendingAE, 1000);
      return true;
    }
    return false;
  }

  private class BatchTable {

    // map request to queued responses
    // Map<Request, BatchEntry>
    private final Map pending = new HashMap(13);

    public boolean add(Request req, Response res) {
      boolean alreadyPending = true;
      synchronized (pending) {
        BatchEntry be = (BatchEntry) pending.get(req);
        if (be == null) {
          alreadyPending = false;
          be = new BatchEntry(res);
          pending.put(req, be);
          if (logger.isInfoEnabled()) {
            logger.info("Send new batch: "+be);
          }
        } else {
          be.add(res);
          if (logger.isInfoEnabled()) {
            logger.info("Batching request: "+be);
          }
        }
      }
      return alreadyPending;
    }

    public AddressEntry findBind(String name, String type) {
      AddressEntry pendingAE = null;
      synchronized (pending) {
        for (Iterator iter = pending.keySet().iterator();
            iter.hasNext();
            ) {
          Request req = (Request) iter.next();
          if (req instanceof Request.Bind) {
            Request.Bind breq = (Request.Bind) req;
            AddressEntry bae = breq.getAddressEntry();
            if (name.equals(bae.getName()) &&
                type.equals(bae.getType())) {
              pendingAE = bae;
              break;
            }
          }
        }
      }
      return pendingAE;
    }

    public boolean setResult(
        Request req, Object result, long ttl) {
      boolean wasPending = true;
      BatchEntry be;
      synchronized (pending) {
        be = (BatchEntry) pending.remove(req);
      }
      // check ttl?
      if (be == null) {
        // ignore
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Ignore non-batched result "+req+" ==> "+result);
        }
        wasPending = false;
      } else {
        int n = be.size();
        if (logger.isInfoEnabled()) {
          logger.info(
              "Setting result for batched "+be+" ==> "+result);
        }
        for (int i = 0; i < n; i++) {
          Response res = be.get(i);
          // set result in a separate thread
          res.setResult(result, ttl);
        }
      }
      return wasPending;
    }

    public boolean remove(Response res) {
      boolean wasListed = false;
      synchronized (pending) {
        Request req = res.getRequest();
        BatchEntry be = (BatchEntry) pending.get(req);
        if (be == null) {
          // ignore
        } else {
          int n = be.size();
          if (n == 0) {
            // shouldn't happen
            pending.remove(req);
          } else {
            for (int i = 0; i < n; i++) {
              Response r = be.get(i);
              if (r == res) {
                wasListed = true;
                if (n == 1) {
                  pending.remove(req);
                } else {
                  be.remove(i);
                }
                break;
              }
            }
          }
        }
      }
      return wasListed;
    }
  }

  private static class BatchEntry {
    public final long batchTime;
    private Object obj;

    public BatchEntry(Response res) {
      batchTime = System.currentTimeMillis();
      obj = res;
    }

    public int size() {
      return 
        ((obj instanceof List) ?
         ((List) obj).size() :
         1);
    }

    public void add(Response res) {
      if (obj instanceof List) {
        List l = (List) obj;
        l.add(res);
      } else {
        List l = new ArrayList(5);
        l.add(obj);
        l.add(res);
        obj = l;
      }
    }

    public void remove(int index) {
      if (obj instanceof List) {
        List l = (List) obj;
        l.remove(index);
      } else {
        throw new UnsupportedOperationException();
      }
    }

    public Response get(int index) {
      if (obj instanceof List) {
        List l = (List) obj;
        return (Response) l.get(index);
      } else {
        return (Response) obj;
      }
    }

    public String toString() {
      return 
        "(batchTime="+
        Timestamp.toString(batchTime)+
        ", responses["+size()+"]"+
        obj+")";
    }
  }

  private class TimeoutHandler
    extends TimerTask
    implements Callback {
      private final Response res;
      public TimeoutHandler(Response res) {
        this.res = res;
      }
      public void execute(Response res) {
        // assert res == this.res;
        // assert res.isAvailable();
        cancel();
      }
      public void run() {
        // this is quick, so we don't need to queue and "restart()"
        // under our own thread.
        timeout(res);
      }
    }
}
