/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mobility.arch.*;
import org.cougaar.core.mobility.ldm.AgentMove;
import org.cougaar.core.mobility.ldm.AgentTransfer;
import org.cougaar.core.mobility.ldm.MobilityFactory;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.GenericStateModel;

/**
 * A node-agent plugin that handles AgentTransfer objects
 * where the target is the local node.
 */
public class RootMobilityPlugin 
extends AbstractMobilityPlugin
{

  // a map from agent MessageAddress to an AgentEntry
  //
  // this is used to guarantee only one move at a time,
  // and hold onto an agent while it's awaiting the
  // move response.
  private final Map entries = new HashMap(13);

  /** a new request for the move of a local agent. */
  protected void addedAgentMove(AgentMove move) {

    if (!(isNode)) return;

    Ticket ticket = move.getTicket();

    MessageAddress id = ticket.getMobileAgent();
    MessageAddress origNode = ticket.getOriginNode();
    MessageAddress destNode = ticket.getDestinationNode();

    if ((id == null) ||
        (id.equals(nodeId))) {
      String s =
        "Move request "+move.getUID()+
        " attempted to move node "+nodeId+
        " -- nodes are not movable!";
      if (log.isErrorEnabled()) {
        log.error(s);
      }
      Throwable stack = new RuntimeException(s);
      move.setStatus(AgentMove.FAILURE_STATUS, stack);
      blackboard.publishChange(move);
      return;
    }

    if ((origNode != null) &&
        (!(nodeId.equals(origNode)))) {
      // FIXME note that this assumes that the agent is
      // on this node, and doesn't do a redirect.
      String s =
        "Move request "+move.getUID()+
        " of agent "+id+
        " is currently on node "+nodeId+
        ", not on the ticket's asserted origin node "+
        origNode;
      if (log.isErrorEnabled()) {
        log.error(s);
      }
      Throwable stack = new RuntimeException(s);
      move.setStatus(AgentMove.FAILURE_STATUS, stack);
      blackboard.publishChange(move);
      return;
    }

    boolean isTrivialMove = false;
    if ((destNode == null) || 
        (nodeId.equals(destNode))) {
      // already at destination node
      isTrivialMove = (!(ticket.isForceRestart()));
    } else {
      // check remote destination node
      //
      // For now we do a quick check to see if the node 
      // is registered in the topology.
      //
      // See bug 1218 for details.
      TopologyEntry te =
        topologyReaderService.getEntryForAgent(
            destNode.getAddress());
      String s;
      if (te == null) {
        s = "Unknown destination node "+destNode;
      } else if (
          te.getType() != TopologyReaderService.NODE_AGENT_TYPE) {
        s = "Destination "+destNode+" is not a node agent"+
          ", it's of type "+te.getTypeAsString();
      } else if (
          te.getStatus() != TopologyEntry.ACTIVE) {
        s = "Destination node "+destNode+" is not active"+
          ", its status is "+te.getStatusAsString();
      } else {
        s = null;
      }
      if (s != null) {
        // destination node is invalid!
        s +=
          ", for move of agent "+id+
          " from "+nodeId+
          " for move "+move.getOwnerUID();
        if (log.isErrorEnabled()) {
          log.error(s);
        }
        Throwable stack = new RuntimeException(s);
        move.setStatus(AgentMove.FAILURE_STATUS, stack);
        blackboard.publishChange(move);
        return;
      }
      if (log.isDebugEnabled()) {
        log.debug(
            "Remote destination node "+destNode+
            " located at "+te);
      }
    }

    // lookup agent in registry, lock in the move
    String errorMsg = null;
    ComponentDescription desc = null;
    Agent agent = null;
    synchronized (entries) {
      // lookup the agent
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae == null) {
        // agent is not known on this node
        errorMsg = 
          "Agent "+id+" is not on node "+nodeId;
      } else if (ae.isMoving) {
        // already moving or arriving!
        errorMsg = 
          "Agent "+id+" on node "+nodeId+
          " is busy with another move request: "+
          ae;
      } else if (!(ae.isRegistered)) {
        // agent is not registered on this node
        errorMsg = 
          "Agent "+id+" on node "+nodeId+
          " is not registered for mobility";
      } else {
        // get the desc and agent from registration
        desc = ae.desc;
        agent = ae.agent;
        // mark as moving
        if (!(isTrivialMove)) {
          ae.isMoving = true;
          ae.move = move;
          ae.transfer = null;
        }
      }
    }

    if (errorMsg != null) {
      if (log.isErrorEnabled()) {
        log.error(errorMsg);
      }
      Throwable stack = new RuntimeException(errorMsg);
      move.setStatus(AgentMove.FAILURE_STATUS, stack);
      blackboard.publishChange(move);
      return;
    }

    if (isTrivialMove) {
      // trivial success -- the agent is already
      // at the destination node
      if (log.isInfoEnabled()) {
        log.info(
            "Agent "+id+" is already at node "+nodeId+
            ", responding with trivial success");
      }
      move.setStatus(AgentMove.SUCCESS_STATUS, null);
      blackboard.publishChange(move);
      return;
    }

    // entries contains this move

    // assume that the agent itself provides the state
    StateObject stateProvider =
      ((agent instanceof StateObject) ?
       ((StateObject) agent) :
       (null));
    // assume that the agent itself regulates its model
    GenericStateModel model = agent;

    MobilitySupportImpl support = 
      new MobilitySupportImpl(
          agent, move, null,
          id, ticket);

    AbstractHandler h;
    if ((destNode == null) ||
        (nodeId.equals(destNode))) {
      h = new DispatchTestHandler(
          support, model, desc, stateProvider);
    } else {
      h = new DispatchRemoteHandler(
          support, model, desc, stateProvider);
    }

    queue(id, h, support);
  }

  /** a move was changed. */
  protected void changedAgentMove(AgentMove move) {
  }

  /** a move was removed. */
  protected void removedAgentMove(AgentMove move) {
    // nothing for now
  }

  /** arrival of a moved agent. */
  protected void addedAgentTransfer(AgentTransfer transfer) {

    if (!(isNode)) return;

    Ticket ticket = transfer.getTicket();

    MessageAddress destNode = ticket.getDestinationNode();
    if (destNode == null) {
      // not expected, since only remote moves
      // create transfers
      if (log.isErrorEnabled()) {
        log.error(
            "Unexpected agent-transfer "+transfer.getUID()+
            " added on node "+nodeId+
            " with null destination node, ticket: "+ticket);
      }
      return;
    } else if (!(nodeId.equals(destNode))) {
      // created by this plugin
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignoring agent transfer to node "+destNode+
            " that doesn't match this node "+nodeId);
      }
      return;
    }

    MessageAddress id = ticket.getMobileAgent();

    // make sure agent is not registered, lock in arrival
    String errorMsg = null;
    synchronized (entries) {
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae == null) {
        ae = new AgentEntry(id);
      }
      if (ae.isMoving) {
        // agent is leaving this node?
        errorMsg = "Unable to accept remote agent "+id+
          ", a move is already in progress: "+ae;
      } else if (ae.isRegistered) {
        // already moving or adding the agent?
        errorMsg = 
          "Unable to accept remote agent "+id+
          ", that agent is already on node "+nodeId+": "+ae;
      } else {
        ae.isMoving = true;
        ae.move = null;
        ae.transfer = transfer;
      }
    }

    if (errorMsg != null) {
      if (log.isErrorEnabled()) {
        log.error(errorMsg);
      }
      Throwable stack = new RuntimeException(errorMsg);
      transfer.setStatus(AgentTransfer.FAILURE_STATUS, stack);
      blackboard.publishChange(transfer);
      return;
    }

    StateTuple state = transfer.getState();

    // remove the completed transfer
    //
    // this may complicate debugging, but it helps ensure
    // GC of captured agent state
    blackboard.publishRemove(transfer);

    MobilitySupportImpl support = 
      new MobilitySupportImpl(
          null, null, transfer,
          id, ticket);

    AbstractHandler h =
      new ArrivalHandler(
          support, state);

    queue(id, h, support);
  }

  /** handle response to a move of a local agent. */
  protected void changedAgentTransfer(AgentTransfer transfer) {

    if (!(isNode)) return;

    Ticket ticket = transfer.getTicket();
    MessageAddress id = ticket.getMobileAgent();

    MessageAddress origNode = ticket.getOriginNode();
    if ((origNode != null) &&
        (!(nodeId.equals(origNode)))) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignore change in transfer "+transfer.getUID()+
            ", intended for origin node "+origNode+
            ", not local node "+nodeId);
      }
      return;
    }

    int status = transfer.getStatusCode();
    if (status == AgentTransfer.NO_STATUS) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignore change with no status for transfer "+
            transfer.getUID());
      }
      return;
    }

    boolean isNack = (status != AgentTransfer.SUCCESS_STATUS);
    Throwable stack = 
      (isNack ? 
       transfer.getFailureStackTrace() :
       null);

    // remove the completed transfer
    //
    // this may complicate debugging, but it helps ensure
    // GC of captured agent state
    blackboard.publishRemove(transfer);

    // make sure agent is not registered, lock in arrival
    String errorMsg = null;
    Agent agent = null;
    synchronized (entries) {
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae == null) {
        // no such move request
        errorMsg = 
          "Unknown agent "+id+" on node "+nodeId+
          ", so unable to process move "+
          (isNack ? "failure" : "success")+
          " response";
      } else if (!(ae.isMoving)) {
        // agent still moving?
        errorMsg = 
          "Agent "+id+" is not moving on node "+nodeId+
          ", so unable to process move "+
          (isNack ? "failure" : "success")+
          " response";
      } else if (!(ae.isRegistered)) {
        // expecting the agent to stay registered during move
        // since unregister is in agent's "stop()".
        errorMsg = 
          "Agent "+id+" on node "+nodeId+
          " is no longer registered";
      } else {
        agent = ae.agent;
        ae.isMoving = true;
        ae.move = null;
        ae.transfer = transfer;
      }
    }

    if (errorMsg != null) {
      if (log.isErrorEnabled()) {
        log.error(errorMsg, stack);
      }
      return;
    }

    // find the move
    UID moveUID = transfer.getOwnerUID();
    AgentMove move = 
      findAgentMove(
          moveUID);
    if (move == null) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+id+
            " move request "+moveUID+
            " for transfer "+ transfer.getUID()+
            " not found in node "+nodeId+"'s blackboard, "+
            " will be unable to set the move status, "+
            " but will complete the move anyways");
      }
    }

    MobilitySupportImpl support = 
      new MobilitySupportImpl(
          agent, move, transfer,
          id, ticket);

    AbstractHandler h;
    if (isNack) {
      h = new NackHandler(support, agent, stack);
    } else {
      h = new AckHandler(support, agent);
    }

    queue(id, h, support);
  }

  /** removal of either the source-side or target-side transfer */
  protected void removedAgentTransfer(AgentTransfer transfer) {
    // nothing for now
  }
  
  /** an agent registers as a mobile agent in the local node. */
  protected void registerAgent(
      MessageAddress id,
      ComponentDescription desc,
      Agent agent) {
    // add entry to the table
    synchronized (entries) {
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae != null) {
        if (ae.isRegistered) {
          throw new RuntimeException(
              "Agent "+id+" is already registered on node "+
              nodeId+": "+ae);
        }
        // must be moving
      } else {
        ae = new AgentEntry(id);
        entries.put(id, ae);
      }
      ae.desc = desc;
      ae.agent = agent;
      ae.isRegistered = true;
    }
  }

  /** an agent unregisters itself from the local mobility registry. */
  protected void unregisterAgent(
      MessageAddress id) {
    synchronized (entries) {
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae == null) {
        // already removed?
        if (log.isErrorEnabled()) {
          log.error(
              "Attempted to unregister agent "+id+
              " on node "+nodeId+
              ", but the agent is not listed");
        }
      } else if (!(ae.isRegistered)) {
        if (log.isErrorEnabled()) {
          log.error(
              "Attempted to unregister agent "+id+
              " on node "+nodeId+
              ", but the agent is not registered");
        }
      } else {
        ae.isRegistered = false;
        if (!(ae.isMoving)) {
          // no longer needed
          entries.remove(id);
        } else {
          // agent is unloading as part of move,
          // keep the entry in case the move fails
        }
      }
    }
  }



  private void queue(
      final MessageAddress id,
      final AbstractHandler h,
      final MobilitySupportImpl support) {
    // ensure cleanup
    Runnable r = new Runnable() {
      public void run() {
        try {
          h.run();
        } finally {
          dequeue(id, h, support);
        }
      }
      public String toString() {
        return h.toString();
      }
    };
    queue(r);
  }

  private void dequeue(
      MessageAddress id,
      AbstractHandler h,
      MobilitySupportImpl support) {
    if (h instanceof DispatchRemoteHandler) {
      // leave the entry, we're waiting for the response.
    } else {
      // remove moving flag
      synchronized (entries) {
        AgentEntry ae = (AgentEntry) entries.get(id);
        if (ae == null) {
          // aborted handler?
        } else {
          ae.isMoving = false;
          if (!(ae.isRegistered)) {
            entries.remove(id);
          } else {
            // keep in table for future moves
          }
        }
      }
    }
    // queue pending blackboard operations
    final List pendingAdds = support.pendingAdds;
    final List pendingChanges = support.pendingChanges;
    if (pendingAdds.isEmpty() && pendingChanges.isEmpty()) {
    } else {
      Runnable r = new Runnable() {
        public void run() {
          for (Iterator iter = pendingAdds.iterator(); 
              iter.hasNext();
              ) {
            Object o = iter.next();
            if (log.isDebugEnabled()) {
              log.debug(
                  "Adding blackboard object "+
                  ((o instanceof UniqueObject) ? 
                   (((UniqueObject) o).getUID().toString()) :
                   "non-unique-object")+" "+o);
            }
            blackboard.publishAdd(o);
          }
          for (Iterator iter = pendingChanges.iterator();
              iter.hasNext();
              ) {
            Object o = iter.next();
            if (log.isDebugEnabled()) {
              log.debug(
                  "Changing blackboard object "+
                  ((o instanceof UniqueObject) ? 
                   (((UniqueObject) o).getUID().toString()) :
                   "non-unique-object")+" "+o);
            }
            blackboard.publishChange(o);
          }
        }
      };
      fireLater(r);
    }
  }

  private class AgentEntry {

    public final MessageAddress id;
    public ComponentDescription desc;
    public Agent agent;

    public boolean isRegistered;

    public boolean isMoving;

    public AgentMove move;
    public AgentTransfer transfer;

    public AgentEntry(MessageAddress id) {
      this.id = id;
    }

    public String toString() {
      return 
        "moving agent "+id+
        ((move != null) ? 
         (" moving with "+move.getTicket()) :
         "");
    }
  }

  private class MobilitySupportImpl 
    extends AbstractMobilitySupport {

      private Agent agent;
      private AgentMove move;
      private AgentTransfer transfer;
      private List pendingAdds = Collections.EMPTY_LIST;
      private List pendingChanges = Collections.EMPTY_LIST;

      public MobilitySupportImpl(
          Agent agent,
          AgentMove move,
          AgentTransfer transfer,
          MessageAddress id,
          Ticket ticket) {
        super(
            id, 
            RootMobilityPlugin.this.nodeId, 
            ticket, 
            RootMobilityPlugin.this.log);
        this.agent = agent;
        this.move = move;
        this.transfer = transfer;
      } 

      public void onDispatch() {
        MessageAddress destNode = ticket.getDestinationNode();
        try {
          Method m = agent.getClass().getMethod(
              "onDispatch",
              new Class[]{MessageAddress.class});
          m.invoke(agent, new Object[]{destNode});
        } catch (MobilityException me) {
          throw me;
        } catch (Exception e) {
          if (RootMobilityPlugin.this.log.isErrorEnabled()) {
            RootMobilityPlugin.this.log.error(
                "Failed agent "+id+" move to node "+destNode, 
                e);
          }
        }
      }

      public void onArrival() {
        if (move != null) {
          move.setStatus(AgentTransfer.SUCCESS_STATUS, null);
          publishChangeLater(move);
        } else {
          if (RootMobilityPlugin.this.log.isWarnEnabled()) {
            RootMobilityPlugin.this.log.warn(
                "Unable to set move status for transfer "+
                ((transfer != null) ? 
                 transfer.getUID().toString() : 
                 "<unknown>"));
          }
        }
      }

      public void onFailure(Throwable throwable) {
        move.setStatus(AgentTransfer.FAILURE_STATUS, throwable);
        publishChangeLater(move);
      }

      public void onRemoval() {
      }

      public void setPendingModel(GenericStateModel model) {
      }

      public GenericStateModel takePendingModel() {
        return null;
      }

      public void sendTransfer(StateTuple tuple) {
        AgentTransfer newTransfer = 
          createAgentTransfer(
              move.getUID(),
              ticket,
              tuple);
        transfer = newTransfer;
        publishAddLater(newTransfer);
      }

      public void sendAck() {
        transfer.setStatus(AgentTransfer.SUCCESS_STATUS, null);
        publishChangeLater(transfer);
      }

      public void sendNack(Throwable throwable) {
        transfer.setStatus(AgentTransfer.FAILURE_STATUS, throwable);
        publishChangeLater(transfer);
      }

      public void addAgent(StateTuple tuple) {
        agentContainer.addAgent(id, tuple);
      }

      public void removeAgent() {
        agentContainer.removeAgent(id);
      }

      private void publishAddLater(Object o) {
        if (pendingAdds == Collections.EMPTY_LIST) {
          pendingAdds = new ArrayList(3);
        }
        pendingAdds.add(o);
      }

      private void publishChangeLater(Object o) {
        if (pendingChanges == Collections.EMPTY_LIST) {
          pendingChanges = new ArrayList(3);
        }
        pendingChanges.add(o);
      }

    }
}
