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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.Scheduled;
import org.cougaar.core.wp.SchedulableWrapper;
import org.cougaar.core.wp.Timestamp;

/**
 * This class watches for bind/unbind requests and maintains
 * the leases in the server.
 */
public class LeaseHandler 
extends HandlerBase 
{
  // see CacheEntriesHandler workaround + bug 2837 + bug 2839
  private static final boolean UPGRADE_GET_TO_GETALL = false;

  private LeaserConfig config;

  private WhitePagesService wps;

  // name -> (type -> lease)
  // Map<String, Map<String, Lease>>
  private final Map leases = new HashMap();

  //
  // renew leases:
  //

  private SchedulableWrapper renewLeasesThread;

  public void setParameter(Object o) {
    this.config = new LeaserConfig(o);
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();

    Scheduled renewLeasesRunner =
      new Scheduled() {
        public void run(SchedulableWrapper thread) {
          renewLeases(thread);
        }
      };
    renewLeasesThread = SchedulableWrapper.getThread(
        threadService,
        renewLeasesRunner,
        "White pages server renew leases");
    renewLeasesThread.schedule(config.checkLeasesPeriod);
  }

  protected Response mySubmit(Response res) {
    // watch for bind/unbind
    Request req = res.getRequest();
    if (req instanceof Request.Get) {
      if (!UPGRADE_GET_TO_GETALL) {
        handle(res, (Request.Get) req);
      }
      return null;
    } else if (req instanceof Request.Bind) {
      return handle(res, (Request.Bind) req);
    } else if (req instanceof Request.Unbind) {
      return handle(res, (Request.Unbind) req);
    } else {
      return res;
    }
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
   * <p>
   * The cache sends a separate "getAll", so we can
   * throw away the "get".
   */
  private void handle(Response res, Request.Get req) {
    String name = req.getName();
    String type = req.getType();
    if (name.endsWith("(MTS)") &&
        "version".equals(type)) {
      // bug 2837
      if (logger.isInfoEnabled()) {
        logger.info(
            "Bug 2837, cancelling bogus MTS version lookup "+req+
            " for the mts-internal name \""+name+"\"");
      }
      res.setResult(null, 1000);
    } else {
      synchronized (leases) {
        Map m = (Map) leases.get(name);
        Lease lease = (m == null ? null : (Lease) m.get(type));
        if (lease != null) {
          AddressEntry pendingAE = lease.findBind(type);
          if (pendingAE != null) {
            if (type.equals("version")) {
              // bug 2837
              if (logger.isInfoEnabled()) {
                logger.info(
                    "Bug 2837, short-cutting local version lookup "+req+
                    " for the pending bind "+pendingAE);
              }
            } else if (name.equals(agentId.getAddress())) {
              // bug 2839
              if (logger.isInfoEnabled()) {
                logger.info(
                    "Bug 2839, short-cutting "+type+" lookup "+req+
                    " for the pending bind "+pendingAE);
              }
            } else {
              if (logger.isWarnEnabled()) {
                logger.warn(
                    "Short-cutting white pages "+req+
                    " that matches a local in-progress (bind "+
                    pendingAE+")");
              }
            }
            res.setResult(pendingAE, 1000);
          }
        }
      }
    }
  }

  private Response handle(Response res, Request.Bind req) {
    if (req.hasOption(Request.CACHE_ONLY)) {
      // bind hints stop here
      res.setResult(new Long(Long.MAX_VALUE), -1);
      return res;
    }

    if (req.isRenewal()) {
      // this had better be our timer!
      //
      // we could make sure by some tricks, e.g. use a custom
      // private Request.Bind subclass.
      if (logger.isDetailEnabled()) {
        logger.detail("leaser ignore self: "+res);
      }
      return res;
    }

    long activeExpire = -1;
    Response ret = res;
    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();
    String type = ae.getType();
    synchronized (leases) {
      Map m = (Map) leases.get(name);
      Lease lease = null;
      if (m == null) {
        // new lease
        m = new HashMap(5);
        leases.put(name, m);
      } else {
        lease = (Lease) m.get(type);
      }
      if (lease == null) {
        // new bind-pending lease
        lease = new Lease(ae);
        lease.addResponse(res);
        m.put(type, lease);
        if (logger.isInfoEnabled()) {
          logger.info("Binding new lease: "+lease);
        }
      } else {
        // found match
        if (ae.equals(lease.getAddressEntry())) {
          // exact match for in-progress or active lease
          if (lease.isBound()) {
            // the lease is active, so we know the status
            activeExpire = lease.getExpirationTime();
            if (logger.isDetailEnabled()) {
              logger.detail("lease already active: "+lease);
            }
          } else {
            // already in progress, we don't know the
            // status yet, so batch with our pending request.
            lease.addResponse(res);
            if (logger.isDetailEnabled()) {
              logger.detail("lease bind in progress: "+lease);
            }
            // batched here
            ret = null;
          }
        } else {
          // cancel old lease, initiate a new one
          //
          // note that a pending bind may be in progress, so we
          // must fail them.
          //
          // create a new lease
          if (logger.isInfoEnabled()) {
            logger.info(
                "Binding replacement entry "+ae+
                " for current lease "+lease);
          }
          // fail the old responses
          int n = lease.numResponses();
          if (n > 0) {
            for (int i = 0; i < n; i++) {
              Response oldRes = lease.getResponse(i);
              // pretend that the pending entry is accepted
              oldRes.setResult(ae, -1);
            }
          }
          // create a new lease and replace the old one
          Lease newLease = new Lease(ae);
          newLease.addResponse(res);
          m.put(type, newLease);
        }
      }
    }
    if (activeExpire > 0) {
      // lease already bound
      res.setResult(new Long(activeExpire), -1);
    }
    return ret;
  }
  
  private Response handle(Response res, Request.Unbind req) {
    // unbind hints stop here
    if (req.hasOption(Request.CACHE_ONLY)) {
      res.setResult(Boolean.TRUE, -1);
      return res;
    }

    AddressEntry ae = req.getAddressEntry();
    String name = ae.getName();
    String type = ae.getType();
    boolean wasBound;
    synchronized (leases) {
      // remove matching lease
      wasBound = false;
      Map m = (Map) leases.get(name);
      if (m != null) {
        Lease l = (Lease) m.get(type);
        if (l != null) {
          AddressEntry lae = l.getAddressEntry();
          if (ae.equals(lae)) {
            // found exact match
            wasBound = true;
            if (m.size() == 1) {
              leases.remove(name);
            } else {
              m.remove(type);
            }
          }
        }
      }
    }

    if (!wasBound) {
      // it's not bound from the local lease's point of view
      //
      // if it's bound in the server without our knowledge,
      // the lease expiration will unbind it for us
      res.setResult(Boolean.TRUE, -1);
    }

    return res;
  }

  protected void myExecute(Request req, Object result, long ttl) {
    if (!(req instanceof Request.Bind)) {
      // assume that unbind worked, or the lack of a lease
      // will make it work
      return;
    }
    Request.Bind rb = (Request.Bind) req;
    AddressEntry ae = rb.getAddressEntry();
    boolean renewal = rb.isRenewal();
    boolean overWrite = rb.isOverWrite();
    String name = ae.getName();
    String type = ae.getType();

    long expireTime;
    boolean success;
    if (result instanceof Long) {
      expireTime = ((Long) result).longValue();
      success = true;
    } else {
      expireTime = -1;
      success = false;
    }

    if (success) {
      if (renewal) {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Lease renewed that expires at "+
              Timestamp.toString(expireTime)+
              " for entry "+ae);
        }
      } else {
        if (logger.isInfoEnabled()) {
          logger.info(
              "Established lease ("+
              (overWrite ? "re" : "")+
              "bind) that expires at "+
              Timestamp.toString(expireTime)+
              " for entry "+ae);
        }
      } 
    } else {
      if (logger.isInfoEnabled()) {
        logger.info(
            "Failed lease ("+
            (overWrite ? "re" : "")+
            "bind), request="+req+
            ", result="+result);
      }
    }

    synchronized (leases) {
      Map m = (Map) leases.get(name);
      Lease lease = null;
      if (m != null) {
        lease = (Lease) m.get(type);
      }
      if (lease != null && 
          ae.equals(lease.getAddressEntry())) {
        // we're expecting this bind-ack
        if (success) {
          // good, we've created a new lease or
          // renewed an existing lease
          long now = System.currentTimeMillis();
          renewed(lease, now, expireTime);
          if (logger.isDebugEnabled()) {
            logger.debug(
                (renewal ? "Renewed" : "Established")+
                " lease="+lease);
          }
        } else {
          // we failed to bind
          m.remove(type);
          if (m.isEmpty()) {
            leases.remove(name);
          }
          if (renewal) {
            if (logger.isWarnEnabled()) {
              logger.warn(
                  "Lost lease renewal!  request="+req+
                  " ttl="+Timestamp.toString(ttl)+
                  " result="+result);
            }

            // FIXME tell agent suicide watcher?

          } else {
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Lost lease creation ("+
                  (overWrite ? "re" : "")+
                  "bind)!  request="+req+
                  " ttl="+Timestamp.toString(ttl)+
                  " result="+result);
            }
          }
        }

        // tell the clients
        lease.setResults(result);
      } else {
        // we must ignore this response
        if (!renewal ||
            (lease != null && !lease.isBound())) {
          // this is a late bind ack for an entry that we're
          // trying to rebind or unbind.  We're waiting for
          // our modification to be delivered and this stale
          // response (or renewal) has raced back.
          //
          // In any case, we don't want this binding, so ignore
          // it.
          if (logger.isInfoEnabled()) {
            logger.info(
                "Ignoring late bind-"+
                (renewal ? "renewal" : "ack")+
                " for wrong entry="+ae+
                ", lease="+lease+
                ", expire="+Timestamp.toString(expireTime));
          }
        } else {
          // highly suspect
          if (logger.isErrorEnabled()) {
            logger.error(
                "Ignoring unexpected non-leased bind-renewal"+
                ", acked-entry="+ae+
                ", expire="+Timestamp.toString(expireTime));
          }
        }
      }
    }
  }

  private void renewLeases(SchedulableWrapper thread) {
    // assert (thread == renewLeasesThread);
    renewLeases();
    // run me again later
    thread.schedule(config.checkLeasesPeriod);
  }

  /**
   * Check our leases and renew them if they'll expire soon.
   * <p>
   * This could be written as a per-lease TimerTask instead of
   * a periodic check.  For now this seems fine...
   */
  private void renewLeases() {
    synchronized (leases) {
      long now = System.currentTimeMillis();
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Check for lease renewal"+
            ", now="+now+
            ", leases["+leases.size()+"]");
      }
      for (Iterator nameIter = leases.values().iterator();
          nameIter.hasNext();
          ) {
        Map m = (Map) nameIter.next();
        for (Iterator typeIter = m.values().iterator();
            typeIter.hasNext();
            ) {
          Lease lease = (Lease) typeIter.next();
          boolean renewNow = shouldRenew(lease, now);
          if (renewNow) {
            Request.Bind rb = new Request.Bind(
                Request.NONE, lease.ae, false, true);
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "Renewing lease that will expire at "+
                  Timestamp.toString(lease.expireTime)+
                  ": "+lease+
                  ", renewRatio: "+config.renewRatio+
                  ", checkLeasesPeriod: "+
                  config.checkLeasesPeriod);
            }
            wps.submit(rb);
          } else {
            if (logger.isDetailEnabled()) {
              logger.detail(
                  "skipping lease rewal"+
                  ", now="+now+", lease="+
                  lease);
            }
          }
        }
      }
    }
  }

  //
  // These are logically part of Lease but require
  // access to the config of the outter class
  //

  private boolean shouldRenew(Lease lease, long now) {
    // calculate renewal time based upon:
    //   expiration time
    //   round-trip time for the last renewal delay
    //   some slack for the above round-trip time
    //   added safety in case the server forgets us
    //
    // here's the current guess:
    //
    // figure out the latest time we could renew
    if (lease.expireTime <= 0) {
      // still pending
      return false;
    }
    long latestRenew =
      lease.expireTime - lease.roundTripTime;
    // weight it to be a little early
    long renewalTime = (long) (
        lease.boundTime +
        (config.renewRatio *
         (latestRenew - lease.boundTime)));
    // adjust for timer period
    renewalTime -= config.checkLeasesPeriod;
    boolean ret = (renewalTime < now);
    if (logger.isDetailEnabled()) {
      logger.detail(
          "shouldRenew ret="+ret+
          " lease="+lease+
          " renewalTime="+
          Timestamp.toString(renewalTime,now)+
          " "+
          (ret ? "<" : ">=") +
          " now="+now);
    }
    if (ret) {
      // renew, mark the sendtime for round-trip measurement
      if (lease.sendTime == 0) {
        lease.sendTime = now;
      }
    }
    return ret;
  }

  private void renewed(Lease lease, long now, long expTime) {
    // set our timestamps
    if (lease.sendTime > 0) {
      long tripTime = now - lease.sendTime;
      // soften this by averaging
      long weightedTripTime;
      if (lease.roundTripTime == 0) {
        // first time
        weightedTripTime = tripTime;
      } else {
        weightedTripTime = (long) (
            config.tripWeight * tripTime +
            (1.0 - config.tripWeight)*lease.roundTripTime);
      }
      lease.roundTripTime = weightedTripTime;
      lease.sendTime = 0;
    }
    lease.boundTime = now;
    lease.expireTime = expTime;
    /** set the result for all pending responses */
  }

  /** config options, soon to be parameters/props */
  private static class LeaserConfig {
    public final double renewRatio;
    public final double tripWeight;
    public final long checkLeasesPeriod = 20*1000;
    public LeaserConfig(Object o) {
      // FIXME parse!
      renewRatio = 
        Double.parseDouble(
            System.getProperty(
              "org.cougaar.core.wp.resolver.lease.renewRation",
              "0.75"));
      tripWeight =
        Double.parseDouble(
            System.getProperty(
              "org.cougaar.core.wp.resolver.lease.tripWeight",
              "0.75"));
    }
  }

  static class Lease {

    private static final List NO_RESPONSES = Collections.EMPTY_LIST;

    public final AddressEntry ae;
    public final long bindTime;
    public long boundTime;
    public long sendTime;
    public long roundTripTime;
    public long expireTime;

    private List responses = NO_RESPONSES;

    public Lease(AddressEntry ae) {
      long now = System.currentTimeMillis();
      this.ae = ae;
      bindTime = now;
      boundTime = 0;
      sendTime = now;
      roundTripTime = 0;
      expireTime = 0;
    }

    public AddressEntry getAddressEntry() {
      return ae;
    }
    public boolean isBound() {
      return boundTime > 0;
    }
    public long getExpirationTime() {
      return expireTime;
    }

    public int numResponses() {
      return responses.size();
    }
    /** find a pending bind with the matching entry type */
    public AddressEntry findBind(String type) {
      AddressEntry pendingAE = null;
      for (int i = 0, n = numResponses(); i < n; i++) {
        Response res = getResponse(i);
        Request req = res.getRequest();
        if (req instanceof Request.Bind) {
          Request.Bind breq = (Request.Bind) req;
          AddressEntry bae = breq.getAddressEntry();
          if (type.equals(bae.getType())) {
            pendingAE = bae;
            break;
          }
        }
      }
      return pendingAE;
    }
    public void addResponse(Response res) {
      if (res == null) {
        throw new IllegalArgumentException("Null res");
      }
      if (isBound()) {
        throw new IllegalStateException(
            "Lease is already bound, not expecting responses: "+
            this);
      }
      if (responses == NO_RESPONSES) {
        responses = new ArrayList(3);
      }
      responses.add(res);
    }
    public Response getResponse(int index) {
      return (Response) responses.get(index);
    }
    /** set the result for all pending responses */
    public void setResults(Object obj) {
      int n = numResponses();
      if (n > 0) {
        for (int i = 0; i < n; i++) {
          Response res = getResponse(i);
          res.setResult(obj, -1);
        }
        clearResponses();
      }
    }
    public void clearResponses() {
      responses = NO_RESPONSES;
    }

    public String toString() {
      long now = System.currentTimeMillis();
      return 
        "(lease"+
        " entry="+ae+
        " bindTime="+
        Timestamp.toString(bindTime,now)+
        " boundTime="+
        Timestamp.toString(boundTime,now)+
        " sendTime="+
        Timestamp.toString(sendTime,now)+
        " roundTripTime="+roundTripTime+
        " expireTime="+
        Timestamp.toString(expireTime,now)+
        " pending["+responses.size()+"]="+responses+
        ")";
    }
  }
}
