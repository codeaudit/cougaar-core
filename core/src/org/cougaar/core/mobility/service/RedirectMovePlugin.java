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

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.util.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.plugin.*;
import org.cougaar.core.service.*;
import org.cougaar.core.mobility.*;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mobility.ldm.TicketIdentifier;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.util.*;

/**
 * The "RedirectMovePlugin" runs in leaf (non node-agent) agents
 * and redirects control requests (AgentControl) to the parent
 * node of this agent.
 * <p>
 * Note: this plugin assumes<ul>
 *   <li>All AgentControls targetted to this agent will be redirected
 *       by this plugin</li>
 *   <li>All AgentControls created by this agent are assumed to
 *       be created by this plugin, unless the control's "ownerUID"
 *       is null.</li> 
 * </ul>
 */
public class RedirectMovePlugin 
extends ComponentPlugin 
{

  private MessageAddress agentId;
  private MessageAddress nodeId;
  private boolean isNode;

  private IncrementalSubscription incomingControlSub;
  private IncrementalSubscription outgoingControlSub;

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

    // agent control requests with this agent as the target
    incomingControlSub = (IncrementalSubscription) 
      blackboard.subscribe(
          createIncomingControlPredicate(agentId));

    // agent control requests with this agent as the source
    outgoingControlSub = (IncrementalSubscription) 
      blackboard.subscribe(
          createOutgoingControlPredicate(agentId));
  }

  protected void execute() {
    if (isNode) {
      return;
    }

    if (incomingControlSub.hasChanged()) {
      // additions
      Enumeration en = incomingControlSub.getAddedList();
      while (en.hasMoreElements()) {
        AgentControl inControl = (AgentControl) en.nextElement();
        addedIncomingControl(inControl);
      }
      // ignore changes: this plugin does them
      // removals
      en = incomingControlSub.getRemovedList();
      while (en.hasMoreElements()) {
        AgentControl inControl = (AgentControl) en.nextElement();
        removedIncomingControl(inControl);
      }
    }

    if (outgoingControlSub.hasChanged()) {
      // ignore additions: this plugin does them
      // changes
      Enumeration en = outgoingControlSub.getChangedList();
      while (en.hasMoreElements()) {
        AgentControl inControl = (AgentControl) en.nextElement();
        changedOutgoingControl(inControl);
      }
      // ignore removals: this plugin does them
    }
  }

  private void addedIncomingControl(AgentControl inControl) {

    // assert (agentId.equals(inControl.getTarget()))

    // redirect to our parent node

    AbstractTicket abstractTicket = inControl.getAbstractTicket();

    if (mobilityFactory == null) {
      String msg =
        "The agent mobility domain (\"mobility\")"+
        " is not available for agent "+
        agentId+", so control request "+inControl.getUID()+
        " has been failed";
      if (log.isErrorEnabled()) {
        log.error(msg);
      }
      inControl.setStatus(
          AgentControl.FAILURE, 
          new RuntimeException(msg));
      blackboard.publishChange(inControl);
      return;
    }

    // expand the ticket
    AbstractTicket fullTicket = abstractTicket;
    if (abstractTicket instanceof MoveTicket) {
      MoveTicket ticket = (MoveTicket) abstractTicket;
      boolean anyMissing = false;
      MessageAddress controlA = ticket.getMobileAgent();
      if (controlA == null) {
	anyMissing = true;
	controlA = agentId;
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
      if (anyMissing) {
	fullTicket = new MoveTicket(
				    ticket.getIdentifier(),
				    controlA,
				    origN,
				    destN,
				    ticket.isForceRestart());
      }
    } else if (abstractTicket instanceof AddTicket) {
      // SARAH!
    } else if (abstractTicket instanceof RemoveTicket) {
    } else {
    }

    AgentControl outControl = 
      mobilityFactory.createAgentControl(
          inControl.getUID(),
          nodeId,
          fullTicket);
    blackboard.publishAdd(outControl);

    if (log.isInfoEnabled()) {
      log.info(
          "Redirected control request for agent "+
          agentId+" to its parent node "+nodeId+
          " (original request: "+inControl.getUID()+
          ", redirected request: "+outControl.getUID()+")");
    }
  }

  private void removedIncomingControl(AgentControl inControl) {
    UID inControlUID = inControl.getUID();
    AgentControl outControl = findOutgoingControl(inControlUID);
    if (outControl != null) {
      // attempt to cancel the control
      if (log.isInfoEnabled()) {
        log.info(
            "Removing in-progress control request for agent "+
            agentId+" that was redirected to its parent node "+
            nodeId+" (original request: "+outControl.getUID()+
            ", redirected request: "+inControlUID+")");
      }
      blackboard.publishRemove(outControl);
    }
  }

  private void changedOutgoingControl(AgentControl outControl) {

    int outControlStatus = outControl.getStatusCode();
    if (outControlStatus == AgentControl.NONE) {
      // not done yet.
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignoring \"no status\" change of control request"+
            " for agent "+agentId+" (uid: "+
            outControl.getUID()+")");
      }
      return;
    }

    UID inControlUID = outControl.getOwnerUID();
    if (inControlUID == null) {
      // the owner tag is null, so this isn't a redirect
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignoring change to control request for agent "+
            agentId+", it is not a redirect (uid: "+
            outControl.getUID()+")");
      }
      return;
    }

    AgentControl inControl = findIncomingControl(
        inControlUID);
    if (inControl == null) {
      // already removed?
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+agentId+" control completed with status "+
            outControl.getStatusCodeAsString()+
            ", but the original control request "+inControlUID+
            " is no longer in the blackboard");
      }
      return;
    }

    if (inControl.getStatusCode() != AgentControl.NONE) {
      // already removed?
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+agentId+" control completed with status "+
            outControl.getStatusCodeAsString()+
            ", but the original control request "+
            inControlUID+
            " already has its status set to "+
            inControl.getStatusCodeAsString());
      }
      return;
    }

    if (log.isDebugEnabled()) {
      log.debug(
          "Copy the new control status "+
          outControl.getStatusCodeAsString()+
          " for agent "+agentId+
          " (from: "+outControl.getUID()+", to:"+
          inControl.getUID()+")");
    }

    inControl.setStatus(
        outControlStatus, 
        outControl.getFailureStackTrace());
    blackboard.publishChange(inControl);

    // we're done with the control now
    blackboard.publishRemove(outControl);

    if (log.isInfoEnabled()) {
      log.info(
          "Updated status of control request for agent "+
          agentId+" to "+inControl.getStatusCodeAsString()+
          " (uid: "+inControl.getUID()+")");
    }
  }

  // could cache this:

  private AgentControl findIncomingControl(
      UID incomingControlUID) {
    return (AgentControl) query(
        incomingControlSub, incomingControlUID);
  }

  private AgentControl findOutgoingControl(
      UID incomingControlUID) {
    if (incomingControlUID != null) {
      Collection real = outgoingControlSub.getCollection();
      int n = real.size();
      if (n > 0) {
        for (Iterator iter = real.iterator(); iter.hasNext(); ) {
          AgentControl control = (AgentControl) iter.next();
          if (incomingControlUID.equals(control.getOwnerUID())) {
            return control;
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

  private static UnaryPredicate createIncomingControlPredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof AgentControl) {
            AgentControl control = (AgentControl) o;
            MessageAddress target = control.getTarget();
            return 
              ((target == null) || 
               agentId.equals(target));
          }
          return false;
        }
      };
  }

  private static UnaryPredicate createOutgoingControlPredicate(
      final MessageAddress agentId) {
    return 
      new UnaryPredicate() {
        public boolean execute(Object o) {
          if (o instanceof AgentControl) {
            AgentControl control = (AgentControl) o;
            MessageAddress source = control.getSource();
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
    this.nodeId = nodeIdService.getMessageAddress();
    getServiceBroker().releaseService(
        this, NodeIdentificationService.class, nodeIdService);
    if (nodeId == null) {
      throw new RuntimeException(
          "Unable to obtain node id");
    }
  }

}
