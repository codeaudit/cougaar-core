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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Request;
import org.cougaar.core.service.wp.Response;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.wp.Timestamp;

/**
 * This class watches for bind/unbind requests and maintains
 * the leases in the server.
 */
public class LeaseHandler 
extends HandlerBase 
{
  private LeaserConfig config;

  private WhitePagesService wps;

  // name -> (type -> lease)
  // Map<String, Map<String, Lease>>
  private final Map leases = new HashMap();

  public void setParameter(Object o) {
    this.config = new LeaserConfig(o);
  }

  public void setWhitePagesService(WhitePagesService wps) {
    this.wps = wps;
  }

  public void load() {
    super.load();
    scheduleRestart(config.checkLeasesPeriod);
  }

  protected Response mySubmit(Response res) {
    // watch for bind/unbind
    Request req = res.getRequest();
    Object ret;
    if (req instanceof Request.Bind) {
      Request.Bind rb = (Request.Bind) req;
      AddressEntry ae = rb.getAddressEntry();
      if (rb.isRenewal()) {
        // this had better be our timer!
        //
        // we could make sure by some tricks, e.g. use a custom
        // private Request.Bind subclass.
        if (logger.isDebugEnabled()) {
          logger.debug("leaser ignore self: "+res);
        }
        ret = null;
      } else {
        ret = bind(ae);
      }
    } else if (req instanceof Request.Unbind) {
      Request.Unbind ru = (Request.Unbind) req;
      AddressEntry ae = ru.getAddressEntry();
      ret = unbind(ae);
    } else {
      ret = null;
    }
    if (ret != null) {
      // okay, we know it's not bound
      res.setResult(ret, -1);
    }
    return res;
  }

  private Object bind(AddressEntry ae) {
    Object ret = null;
    String n = ae.getName();
    String type = ae.getType();
    synchronized (leases) {
      Map m = (Map) leases.get(n);
      Lease lease = null;
      if (m == null) {
        // new lease
        m = new HashMap(5);
        leases.put(n, m);
      } else {
        lease = (Lease) m.get(type);
      }
      if (lease == null) {
        // new bind-pending lease
        lease = new Lease(ae);
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
            ret = new Long(lease.getExpirationTime());
          } else {
            // already in progress, we don't know the
            // status yet, so pass down to the
            // batch-layer.  This might race but it's
            // harmless
          }
        } else {
          // cancel old lease, initiate a new one
          //
          // note that the old renewal may be in progress,
          // but we'll catch that case in the execute handling
          //
          // recycle lease, set bind time to now
          if (logger.isInfoEnabled()) {
            logger.info(
                "Binding replacement entry "+ae+
                " for current lease "+lease);
          }
          lease.recycle(ae);
        }
      }
    }
    return ret;
  }

  private Object unbind(AddressEntry ae) {
    String n = ae.getName();
    String type = ae.getType();
    boolean wasBound;
    synchronized (leases) {
      // remove matching lease
      wasBound = false;
      Map m = (Map) leases.get(n);
      if (m != null) {
        Lease l = (Lease) m.get(type);
        if (l != null) {
          AddressEntry lae = l.getAddressEntry();
          if (ae.equals(lae)) {
            // found exact match
            wasBound = true;
            if (m.size() == 1) {
              leases.remove(n);
            } else {
              m.remove(type);
            }
          }
        }
      }
    }
    if (wasBound) {
      // we bound this, so we must send the unbind
      return null;
    } else {
      // it's not bound from the local lease's point of view
      //
      // if it's bound in the server without our knowledge,
      // the lease expiration will unbind it for us
      return Boolean.TRUE;
    }
  }

  protected void myExecute(Request req, Object result, long ttl) {
    if (!(req instanceof Request.Bind)) {
      // assume that unbind worked, or the lack of a lease
      // will make it work
      return;
    }
    // should we check that we actually sent a "bind"
    // request?  let's trust the server...
    Request.Bind rb = (Request.Bind) req;
    AddressEntry ae = rb.getAddressEntry();
    boolean overWrite = rb.isOverWrite();
    boolean renewal = rb.isRenewal();
    if (result instanceof Long) {
      long expireTime = ((Long) result).longValue();
      if (logger.isInfoEnabled()) {
        logger.info(
            "Lease "+
            (renewal ?
             "renewed" :
             ("created ("+
              (overWrite ? "re" : "")+
              "bind)"))+
            " that expires at "+
            Timestamp.toString(expireTime)+
            " for entry "+ae);
      }
      bound(ae, renewal, expireTime);
    } else {
      // failure?
      //
      // FIXME tell the agent suicide watch
      if (logger.isErrorEnabled()) {
        logger.error(
            "Lost lease "+
            (renewal ? 
             "renewal" : 
             ("creation ("+
             (overWrite ? "re" : "")+
             "bind)"))+
            "!  req="+req+
            " entry="+ae+
            " ttl="+Timestamp.toString(ttl)+
            " result="+result);
      }
      unbind(ae);
    }
  }

  private void bound(
      AddressEntry ae, boolean renewal, long expireTime) {
    String n = ae.getName();
    String type = ae.getType();
    synchronized (leases) {
      Map m = (Map) leases.get(n);
      Lease lease = null;
      if (m != null) {
        lease = (Lease) m.get(type);
      }
      if (lease != null && 
          ae.equals(lease.getAddressEntry())) {
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
          if (logger.isDebugEnabled()) {
            logger.debug(
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

  protected void myRun() {
    renewLeases();
    // run me again later
    scheduleRestart(config.checkLeasesPeriod);
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
      if (logger.isInfoEnabled()) {
        logger.info(
            "renewLeases now="+now+
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
          if (logger.isDebugEnabled()) {
            logger.debug(
                (renewNow ? "renew" : "skip")+
                " now="+now+", lease="+
                lease);
          }
          if (renewNow) {
            Request.Bind rb = new Request.Bind(
                false, 0, lease.ae, false, true);
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Renew lease that will expire at "+
                  Timestamp.toString(lease.expireTime)+
                  ": "+lease+
                  ", renewRatio: "+config.renewRatio+
                  ", checkLeasesPeriod: "+
                  config.checkLeasesPeriod);
            }
            wps.submit(rb);
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
    if (logger.isDebugEnabled()) {
      logger.debug(
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
    public AddressEntry ae;
    public long bindTime;
    public long boundTime;
    public long sendTime;
    public long roundTripTime;
    public long expireTime;
    public Lease(AddressEntry ae) {
      recycle(ae);
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
    public void recycle(AddressEntry ae) {
      long now = System.currentTimeMillis();
      this.ae = ae;
      bindTime = now;
      boundTime = 0;
      sendTime = now;
      roundTripTime = 0;
      expireTime = 0;
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
        ")";
    }
  }
}
