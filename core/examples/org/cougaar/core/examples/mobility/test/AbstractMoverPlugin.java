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

package org.cougaar.core.examples.mobility.test;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.CollectionSubscription;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.MoveAgent;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.DomainService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * Base class for simple mobility testing.
 *
 * @see QuickMoverPlugin 
 */
public abstract class AbstractMoverPlugin
extends ComponentPlugin 
{

  protected MessageAddress agentId;

  protected LoggingService log;

  protected MobilityFactory mobilityFactory;

  protected IncrementalSubscription moveAgentSub;

  public void setLoggingService(LoggingService log) {
    this.log = log;
  }

  // get the mobility factory via service
  public void setDomainService(DomainService domain) {

     // get the mobility factory
     this.mobilityFactory = (MobilityFactory) 
       domain.getFactory("mobility");

     if (mobilityFactory == null) {
       // see CDG section 5.2.5
       throw new RuntimeException(
          "Mobility domain not loaded -- check your \""+
          "LDMDomain.ini\" configuration");
     }
  }

  protected void setupSubscriptions() {
    agentId = getClusterIdentifier();

    // watch for changed MoveAgent objects
    moveAgentSub = (IncrementalSubscription) 
      blackboard.subscribe(
          createMovePredicate(agentId));
  }

  protected void execute() {
    if (moveAgentSub.hasChanged()) {
      // watch for changes
      Enumeration en = moveAgentSub.getChangedList();
      while (en.hasMoreElements()) {
        MoveAgent ma = (MoveAgent) en.nextElement();
        MoveAgent.Status mstat = ma.getStatus();
        if (mstat != null) {
          handleCompletedMove(ma);
        }
      }
    }
  }

  protected abstract void handleCompletedMove(MoveAgent ma);

  protected MoveAgent findMoveAgent(UID moveAgentUID) {
    Collection real = moveAgentSub.getCollection();
    int n = real.size();
    if (n > 0) {
      for (Iterator iter = real.iterator(); iter.hasNext(); ) {
        MoveAgent ma = (MoveAgent) iter.next();
        if (ma.getUID().equals(moveAgentUID)) {
          return ma;
        }
      }
    }
    return null;
  }

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
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof MoveAgent) {
            MoveAgent ma = (MoveAgent) o;
            return (agentId.equals(ma.getSource()));
          }
          return false;
        }
      };
  }

  protected Ticket createTicket(
       String mobileAgent,   // name of agent to move
       String origNode,  // node where the agent is at right now
       String destNode, // destination for the move
       boolean forceRestart  // restart even if at the destination
       ) {

     // create addresses
     ClusterIdentifier mobileAgentId = 
       ((mobileAgent != null) ? 
        (ClusterIdentifier.getClusterIdentifier(mobileAgent)) :
        null);
     MessageAddress origNodeId =
       ((origNode != null) ?
        (new MessageAddress(origNode)) :
        null);
     MessageAddress destNodeId = 
       ((destNode != null) ?
        (new MessageAddress(destNode)) :
        null);

     return createTicket(
         mobileAgentId,
         origNodeId,
         destNodeId,
         forceRestart);
  }

  protected Ticket createTicket(
       MessageAddress mobileAgentId,   // name of agent to move
       MessageAddress origNodeId,  // node where the agent is at right now
       MessageAddress destNodeId, // destination for the move
       boolean forceRestart  // restart even if at the destination
       ) {

     // create move "ticket"
     Object ticketId = mobilityFactory.createTicketIdentifier();
     Ticket ticket = 
        new Ticket(
          ticketId,
          mobileAgentId,
          origNodeId,
          destNodeId,
          forceRestart);

     return ticket;
  }

  protected void requestMove(Ticket ticket) {
    // create MoveAgent object
    MoveAgent ma = mobilityFactory.createMoveAgent(ticket);
    blackboard.publishAdd(ma);
  }
}
