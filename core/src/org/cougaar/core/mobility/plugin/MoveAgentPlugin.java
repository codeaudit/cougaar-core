/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.mobility.plugin;

import java.io.Serializable;
import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.mobility.*;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mobility.ldm.TicketIdentifier;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.*;

/**
 * The "MoveAgentPlugin" watches for "MoveAgent" blackboard
 * objects and submits the move request, then fills in the
 * response when the move completes (or fails).
 */
public class MoveAgentPlugin 
extends ComponentPlugin 
implements MobilityListener
{

  private MessageAddress self;
  private IncrementalSubscription moveAgentSub;

  private BufferedMobilityListener mlBuffer;
  private MobilityListener mlCallback;

  private LoggingService log;

  private MobilityDispatchService mobilityDispatchService;
  private MobilityListenerService mobilityListenerService;

  public void load() {
    super.load();

    self = getClusterIdentifier();

    log = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Loading the MoveAgent plugin in agent "+self);
    }

    mlCallback = new MyMobilityListener();

    mlBuffer = new BufferedMobilityListener(self);

    mobilityListenerService = (MobilityListenerService)
      getServiceBroker().getService(
          this, MobilityListenerService.class, null);
    if (mobilityListenerService == null) {
      // should we check again later?
      if (log.isErrorEnabled()) {
        log.error(
            "Unable to get the mobility listener service, "+
            "will fail all move requests");
      }
    }

    mobilityDispatchService = (MobilityDispatchService)
      getServiceBroker().getService(
          this, MobilityDispatchService.class, null);
    if (mobilityDispatchService == null) {
      // should we check again later?
      if (log.isErrorEnabled()) {
        log.error(
            "Unable to get the mobility dispatch service, "+
            "will fail all move requests");
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Loaded");
    }
  }

  public void unload() {
    if (mobilityDispatchService != null) {
      getServiceBroker().releaseService(
          this, MobilityDispatchService.class, mobilityDispatchService);
      mobilityDispatchService = null;
    }
    if (mobilityListenerService != null) {
      getServiceBroker().releaseService(
          this, MobilityListenerService.class, mobilityListenerService);
      mobilityListenerService = null;
    }
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    UnaryPredicate pred = createMovePredicate(self);
    moveAgentSub = (IncrementalSubscription) blackboard.subscribe(pred);

    if (!(blackboard.didRehydrate())) {
      checkMoveResults();
    }

    checkMoveResults();
  }

  protected void execute() {
    if (log.isDebugEnabled()) {
      log.debug("Execute");
    }
    checkMoveRequests();
    checkMoveResults();
  }

  private void checkMoveResults() {
    // deliver from buffer
    mlBuffer.deliver(mlCallback);
  }

  // HACK!
  private boolean forceExecute = false;

  protected boolean shouldExecute() {
    boolean b = forceExecute;
    forceExecute = false;
    return (super.shouldExecute() || b);
  }

  private void requestExecute() {
    forceExecute = true;
    requestCycle();
  }

  public MessageAddress getAddress() {
    return self;
  }

  // send to the buffer -- we'll pick them up during "execute()"
  //
  // FIXME should we block until the "execute()", to make sure
  //   that the agent doesn't move until these buffered events
  //   are handled?  This would be done with a wait/notify.
  public void onDispatch(Ticket ticket) {
    if (log.isDebugEnabled()) {
      log.debug("onDispatch (buffered) for ticket "+ticket.getIdentifier());
    }
    mlBuffer.onDispatch(ticket);
    requestExecute();
  }
  public void onArrival(Ticket ticket) {
    if (log.isDebugEnabled()) {
      log.debug("onArrival (buffered) for ticket "+ticket.getIdentifier());
    }
    mlBuffer.onArrival(ticket);
    requestExecute();
  }
  public void onFailure(Ticket ticket, Throwable throwable) {
    if (log.isDebugEnabled()) {
      log.debug("onFailure (buffered) for ticket "+ticket.getIdentifier());
    }
    mlBuffer.onFailure(ticket, throwable);
    requestExecute();
  }

  // my mobility listener, which is only called within "execute()":
  private class MyMobilityListener implements MobilityListener {

    public MessageAddress getAddress() {
      return self;
    }

    public void onDispatch(Ticket ticket) {
      Object id = ticket.getIdentifier();
      if (log.isInfoEnabled()) {
        log.info("Ignore agent dispatch "+id);
      }
    }

    public void onArrival(Ticket ticket) {
      Object id = ticket.getIdentifier();
      if (log.isInfoEnabled()) {
        log.info("Handling agent arrival "+id);
      }
      MoveAgent ma = findMoveAgent(ticket);
      if (ma == null) {
        return;
      }
      MoveAgent.Status status =
        new MoveAgent.Status(
            MoveAgent.Status.OKAY, 
            "Agent arrived at time "+
            System.currentTimeMillis(),
            null);
      ma.setStatus(status);
      blackboard.publishChange(ma);
      if (log.isInfoEnabled()) {
        log.info(
            "MoveAgent object \""+ma.getUID()+"\" status updated");
      }
    }

    public void onFailure(Ticket ticket, Throwable throwable) {
      Object id = ticket.getIdentifier();
      if (log.isInfoEnabled()) {
        log.info("Handling failed agent move "+id);
      }
      MoveAgent ma = findMoveAgent(ticket);
      if (ma == null) {
        return;
      }
      MoveAgent.Status status =
        new MoveAgent.Status(
          MoveAgent.Status.FAILURE, 
          "Failed at time "+
          System.currentTimeMillis(),
          throwable);
      ma.setStatus(status);
      blackboard.publishChange(ma);
      if (log.isInfoEnabled()) {
        log.info(
            "MoveAgent object \""+ma.getUID()+"\" status updated");
      }
    }

    private MoveAgent findMoveAgent(Ticket ticket) {
      Object id = ticket.getIdentifier();
      // get the MoveAgent object's UID
      if (!(id instanceof TicketIdentifier)) {
        if (log.isErrorEnabled()) {
          log.error(
              "Illegal ticket with identifier "+id+
              " was not created by the MobilityFactory");
        }
        return null;
      }
      UID uid = ((TicketIdentifier) id).getUID();
      // find the corresponding MoveAgent object
      MoveAgent ma = (MoveAgent) query(moveAgentSub, uid);
      if (ma == null) {
        if (log.isErrorEnabled()) {
          log.error(
              "Unable to find MoveAgent object with uid "+uid);
        }
        return null;
      }
      return ma;
    }
  }

  private void checkMoveRequests() {
    if (moveAgentSub.hasChanged()) {
      Enumeration en = moveAgentSub.getAddedList();
      while (en.hasMoreElements()) {
        MoveAgent ma = (MoveAgent) en.nextElement();
        submitMove(ma);
      }
    }
  }

  private void submitMove(MoveAgent ma) {

    Ticket ticket = ma.getTicket();

    if (log.isInfoEnabled()) {
      log.info(
          "Received new MoveAgent object with ticket \""+
          ticket.getIdentifier()+"\"");
    }

    try {

      if ((mobilityDispatchService == null) ||
          (mobilityListenerService == null)) {
        throw new RuntimeException(
            "Unable to submit move (no mobility services)");
      }

      mobilityDispatchService.dispatch(ticket);

    } catch (Exception e) {
      String msg =
        "MoveAgent plugin failed \"dispatch\"";
      if (log.isErrorEnabled()) {
        log.error(msg, e);
      }
      MoveAgent.Status status =
        new MoveAgent.Status(
            MoveAgent.Status.FAILURE, 
            msg,
            e);
      ma.setStatus(status);
      blackboard.publishChange(ma);
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Submitted move for ticket \""+
          ticket.getIdentifier()+"\"");
    }
  }

  // should be an easier way...
  // maybe add a new CollectionSubscription method?
  private static UniqueObject query(CollectionSubscription sub, UID uid) {
    Collection real = sub.getCollection();
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        Object o = iter.next();
        if (o instanceof UniqueObject) {
          UniqueObject uo = (UniqueObject) o;
          UID x = uo.getUID();
          if (uid.equals(x)) {
            return uo;
          }
        }
      }
    }
    return null;
  }

  private static UnaryPredicate createMovePredicate(
      final MessageAddress self) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof MoveAgent) {
            MoveAgent ma = (MoveAgent) o;
            Ticket ticket = ma.getTicket();
            if (ticket != null) {
              MessageAddress a = ticket.getMobileAgent();
              return ((a == null) || a.equals(self));
            }
          }
          return false;
        }
      };
  }

}
