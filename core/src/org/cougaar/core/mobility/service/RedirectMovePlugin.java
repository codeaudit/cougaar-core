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
package org.cougaar.core.mobility.service;

import java.io.Serializable;
import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.core.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.mobility.*;
import org.cougaar.core.mobility.ldm.AgentMove;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.TicketIdentifier;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.util.*;

/**
 * The "RedirectMovePlugin" runs in leaf (non node-agent) agents
 * and redirects move requests (AgentMove) to the parent
 * node of this agent.
 * <p>
 * Note: this plugin assumes<ul>
 *   <li>All AgentMoves targetted to this agent will be redirected
 *       by this plugin</li>
 *   <li>All AgentMoves created by this agent are assumed to
 *       be created by this plugin, unless the move's "ownerUID"
 *       is null.</li> 
 * </ul>
 */
public class RedirectMovePlugin 
extends ComponentPlugin 
{

  private MessageAddress agentId;
  private MessageAddress nodeId;
  private boolean isNode;

  private IncrementalSubscription incomingMoveSub;
  private IncrementalSubscription outgoingMoveSub;

  private MobilityFactory mobilityFactory;

  private LoggingService log;

  public void load() {
    super.load();

    log = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    getAgentId();
    getNodeId();

    isNode = agentId.equals(nodeId);
    if (isNode) {
      // accidentially loaded into node?
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Loading the redirect-move-plugin in agent "+
          agentId+" on node "+nodeId);
    }

    // get the mobility factory
    DomainService domain = (DomainService)
      getServiceBroker().getService(
          this, DomainService.class, null);
    if (domain == null) {
      throw new RuntimeException(
          "Unable to obtain the domain service");
    }
    mobilityFactory = (MobilityFactory) 
      domain.getFactory("mobility");
    if (mobilityFactory == null) {
      if (log.isWarnEnabled()) {
        log.warn(
            "The agent mobility domain (\"mobility\") for agent "+
            agentId+" on node "+nodeId+" has been disabled.");
      }
    }
    getServiceBroker().releaseService(
        this, DomainService.class, domain);

    if (log.isDebugEnabled()) {
      log.debug("Loaded");
    }
  }

  public void unload() {
    if ((log != null) &&
        (log != LoggingService.NULL)) {
      getServiceBroker().releaseService(
          this, LoggingService.class, log);
      log = LoggingService.NULL;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    if (isNode) {
      // disabled if loaded into node
      return;
    }

    // agent move requests with this agent as the target
    incomingMoveSub = (IncrementalSubscription) 
      blackboard.subscribe(
          createIncomingMovePredicate(agentId));

    // agent move requests with this agent as the source
    outgoingMoveSub = (IncrementalSubscription) 
      blackboard.subscribe(
          createOutgoingMovePredicate(agentId));
  }

  protected void execute() {
    if (isNode) {
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug("Execute");
    }

    if (incomingMoveSub.hasChanged()) {
      // additions
      Enumeration en = incomingMoveSub.getAddedList();
      while (en.hasMoreElements()) {
        AgentMove inMove = (AgentMove) en.nextElement();
        addedIncomingMove(inMove);
      }
      // ignore changes: this plugin does them
      // removals
      en = incomingMoveSub.getRemovedList();
      while (en.hasMoreElements()) {
        AgentMove inMove = (AgentMove) en.nextElement();
        removedIncomingMove(inMove);
      }
    }

    if (outgoingMoveSub.hasChanged()) {
      // ignore additions: this plugin does them
      // changes
      Enumeration en = outgoingMoveSub.getChangedList();
      while (en.hasMoreElements()) {
        AgentMove inMove = (AgentMove) en.nextElement();
        changedOutgoingMove(inMove);
      }
      // ignore removals: this plugin does them
    }
  }

  private void addedIncomingMove(AgentMove inMove) {

    // assert (agentId.equals(inMove.getTarget()))

    // redirect to our parent node

    MoveTicket ticket = inMove.getTicket();

    if (log.isInfoEnabled()) {
      log.info(
          "Redirecting agent mobility request "+
          inMove.getUID()+" for agent "+
          agentId+" to the parent node "+nodeId);
    }

    if (mobilityFactory == null) {
      String msg =
        "The agent mobility domain (\"mobility\")"+
        " is not available for agent "+
        agentId+", so move request "+inMove.getUID()+
        " has been failed";
      if (log.isErrorEnabled()) {
        log.error(msg);
      }
      inMove.setStatus(
          AgentMove.FAILURE_STATUS, 
          new RuntimeException(msg));
      blackboard.publishChange(inMove);
      return;
    }

    // FIXME - only supports moves 
    
    // expand the ticket
    boolean anyMissing = false;
    MessageAddress moveA = ticket.getMobileAgent();
    if (moveA == null) {
      anyMissing = true;
      moveA = agentId;
    }
    MessageAddress origN = ticket.getOriginNode();
    if (origN == null) {
      anyMissing = true;
      origN = nodeId;
    }
    MessageAddress destN = ticket.getDestinationNode();
    if (destN == null) {
      anyMissing = true;
      destN = nodeId;
    }
    MoveTicket fullTicket = ticket;
    if (anyMissing) {
      fullTicket = new MoveTicket(
          ticket.getIdentifier(),
          moveA,
          origN,
          destN,
          ticket.isForceRestart());
    }

    AgentMove outMove = 
      mobilityFactory.createAgentMove(
          inMove.getUID(),
          nodeId,
          fullTicket);
    blackboard.publishAdd(outMove);

    if (log.isInfoEnabled()) {
      log.info(
          "Submitted redirected move request "+
          outMove.getUID()+
          " for agent "+agentId+" to node "+nodeId+
          ", original move request is "+inMove.getUID());
    }
  }

  private void removedIncomingMove(AgentMove inMove) {
    UID inMoveUID = inMove.getUID();
    AgentMove outMove = findOutgoingMove(inMoveUID);
    if (outMove != null) {
      // attempt to cancel the move
      if (log.isInfoEnabled()) {
        log.info(
            "Removing in-progress move request "+outMove.getUID()+
            " redirected from original request "+inMoveUID);
      }
      blackboard.publishRemove(outMove);
    }
  }

  private void changedOutgoingMove(AgentMove outMove) {

    int outMoveStatus = outMove.getStatusCode();
    if (outMoveStatus == AgentMove.NO_STATUS) {
      // not done yet.
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignoring \"no status\" change of move request "+
            outMove.getUID()+" for agent "+agentId);
      }
      return;
    }

    UID inMoveUID = outMove.getOwnerUID();
    if (inMoveUID == null) {
      // we need a better tag!
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignoring change to move "+outMove.getUID()+
            ", it has a null ownerUID and is therefore"+
            " not a redirect");
      }
      return;
    }

    AgentMove inMove = findIncomingMove(
        inMoveUID);
    if (inMove == null) {
      // already removed?
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+agentId+" move completed with status "+
            outMove.getStatusCodeAsString()+
            ", but the original move request "+inMoveUID+
            " is no longer in the blackboard");
      }
      return;
    }

    if (inMove.getStatusCode() != AgentMove.NO_STATUS) {
      // already removed?
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+agentId+" move completed with status "+
            outMove.getStatusCodeAsString()+
            ", but the original move request "+
            inMoveUID+
            " already has its status set to "+
            inMove.getStatusCodeAsString());
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Copying redirected agent "+agentId+" move status "+
          outMove.getStatusCodeAsString()+
          " from "+outMove.getUID()+" to original "+
          inMove.getUID());
    }

    inMove.setStatus(
        outMoveStatus, 
        outMove.getFailureStackTrace());
    blackboard.publishChange(inMove);

    // we're done with the move now
    blackboard.publishRemove(outMove);

    if (log.isInfoEnabled()) {
      log.info(
          "Original move request \""+inMove.getUID()+
          "\" status updated to "+inMove.getStatusCodeAsString());
    }
  }

  // could cache this:

  private AgentMove findIncomingMove(
      UID incomingMoveUID) {
    return (AgentMove) query(
        incomingMoveSub, incomingMoveUID);
  }

  private AgentMove findOutgoingMove(
      UID incomingMoveUID) {
    if (incomingMoveUID != null) {
      Collection real = outgoingMoveSub.getCollection();
      int n = real.size();
      if (n > 0) {
        for (Iterator iter = real.iterator(); iter.hasNext(); ) {
          AgentMove move = (AgentMove) iter.next();
          if (incomingMoveUID.equals(move.getOwnerUID())) {
            return move;
          }
        }
      }
    }
    return null;
  }

  private static UniqueObject query(CollectionSubscription sub, UID uid) {
    if (uid != null) {
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
    }
    return null;
  }

  private static UnaryPredicate createIncomingMovePredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof AgentMove) {
            AgentMove move = (AgentMove) o;
            MessageAddress target = move.getTarget();
            return 
              ((target == null) || 
               agentId.equals(target));
          }
          return false;
        }
      };
  }

  private static UnaryPredicate createOutgoingMovePredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof AgentMove) {
            AgentMove move = (AgentMove) o;
            MessageAddress source = move.getSource();
            return 
              ((source == null) ||
               (agentId.equals(source)));
          }
          return false;
        }
      };
  }

  private void getAgentId() {
    // get the agentId
    AgentIdentificationService agentIdService = 
      (AgentIdentificationService)
      getServiceBroker().getService(
          this,
          AgentIdentificationService.class,
          null);
    if (agentIdService == null) {
      throw new RuntimeException(
          "Unable to obtain agent-id service");
    }
    this.agentId = agentIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, AgentIdentificationService.class, agentIdService);
    if (agentId == null) {
      throw new RuntimeException(
          "Unable to obtain agent id");
    }
  }

  private void getNodeId() {
    // get the nodeId
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      getServiceBroker().getService(
          this,
          NodeIdentificationService.class,
          null);
    if (nodeIdService == null) {
      throw new RuntimeException(
          "Unable to obtain node-id service");
    }
    this.nodeId = nodeIdService.getNodeIdentifier();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }
  }

}
