/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.MobilityException;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.RemoveTicket;
import org.cougaar.core.mobility.arch.AbstractHandler;
import org.cougaar.core.mobility.arch.AckHandler;
import org.cougaar.core.mobility.arch.ArrivalHandler;
import org.cougaar.core.mobility.arch.DispatchRemoteHandler;
import org.cougaar.core.mobility.arch.DispatchTestHandler;
import org.cougaar.core.mobility.arch.MobilitySupport;
import org.cougaar.core.mobility.arch.NackHandler;
import org.cougaar.core.mobility.ldm.AgentControl;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.Application;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.GenericStateModel;

/**
 * A node-agent plugin that handles AgentControl objects
 * where the target is the local node.
 */
public class RootMobilityPlugin 
extends AbstractMobilityPlugin
{

  // a map from agent MessageAddress to an AgentEntry
  //
  // this is used to guarantee only one control at a time,
  // and hold onto an agent while it's awaiting the
  // control response.
  private final Map entries = new HashMap(13);

  /** a new request for the control of a local agent. */
  protected void addedAgentControl(AgentControl control) {
    if (!(isNode)) return;

    AbstractTicket abstractTicket = control.getAbstractTicket();

    if (log.isDebugEnabled()) {
      log.debug("Observed add of "+control.getUID());
    }

    if (abstractTicket instanceof AddTicket) {

      AddTicket addTicket = (AddTicket) abstractTicket;
      MessageAddress id = addTicket.getMobileAgent();
      MessageAddress destNode = addTicket.getDestinationNode();

      // check if this node is the destination node
      if ((destNode != null) && (!destNode.equals(nodeId))) {
        // not for me!  let the RedirectMovePlugin forward the request
        // to the other node.
        return;
      }

      // FIXME consider locking in registry, to prevent multiple
      //   simultaneous add/removes

      // create the component-desc that we'll add
      ComponentDescription desc = new ComponentDescription(
          "org.cougaar.core.agent.ClusterImpl",
          "Node.AgentManager.Agent",
          "org.cougaar.core.agent.ClusterImpl",
          null,
          id,
          null, null, null,
          ComponentDescription.PRIORITY_COMPONENT);

      Object state = null;  // no prior state
      StateTuple tuple = new StateTuple(desc, state);

      // run outside this transaction, to 
      //   a) prevent blocking, and
      //   b) avoid nested transactions (bug 1750)
      AddAgentRunner aar = 
        new AddAgentRunner(id, control, tuple);
      queue(id, aar, aar.pendingTuples);

    } else if (abstractTicket instanceof RemoveTicket) {

      // handle remove

      RemoveTicket removeTicket = (RemoveTicket) abstractTicket;
      MessageAddress id = removeTicket.getMobileAgent();
      MessageAddress destNode = removeTicket.getDestinationNode();

      // check if this node is the destination node
      if ((destNode != null) && (!destNode.equals(nodeId))) {
        // not for me!  let the RedirectMovePlugin forward the request
        // to the other node.
        return;
      }

      // FIXME consider locking in registry, to prevent multiple
      //   simultaneous add/removes

      // run outside this transaction, to 
      //   a) prevent blocking, and
      //   b) avoid nested transactions (bug 1750)
      RemoveAgentRunner rar = 
        new RemoveAgentRunner(id, control);
      queue(id, rar, rar.pendingTuples);

    } else if (abstractTicket instanceof MoveTicket) {

      MoveTicket moveTicket = (MoveTicket) abstractTicket;
      MessageAddress id = moveTicket.getMobileAgent();
      MessageAddress origNode = moveTicket.getOriginNode();
      MessageAddress destNode = moveTicket.getDestinationNode();

      if ((id == null) ||
          (id.equals(nodeId))) {
        String s =
          "Move request "+control.getUID()+
          " attempted to move node "+nodeId+
          " -- nodes are not movable!";
        if (log.isErrorEnabled()) {
          log.error(s);
        }
        Throwable stack = new RuntimeException(s);
        control.setStatus(AgentControl.FAILURE, stack);
        blackboard.publishChange(control);
        return;
      }

      if ((origNode != null) &&
          (!(nodeId.equals(origNode)))) {
        // FIXME note that this assumes that the agent is
        // on this node, and doesn't do a redirect.
        String s =
          "Agent "+id+
          " is currently on node "+nodeId+
          ", not on the ticket's asserted origin node "+origNode+
          " (uid: "+control.getUID()+")";
        if (log.isErrorEnabled()) {
          log.error(s);
        }
        Throwable stack = new RuntimeException(s);
        control.setStatus(AgentControl.FAILURE, stack);
        blackboard.publishChange(control);
        return;
      }

      boolean isTrivialMove = false;
      if ((destNode == null) || 
          (nodeId.equals(destNode))) {
        // already at destination node
        isTrivialMove = (!(moveTicket.isForceRestart()));
      } else {
        // check remote destination node
        //
        // For now we do a quick check to see if the node 
        // is registered in the WP.
        //
        // See bug 1218 for details.
        String s = null;
        AddressEntry ae = null;
        try {
          ae = whitePagesService.get(
              destNode.getAddress(),
              Application.getApplication("topology"),
              "node",
              (30000)); // 30 seconds
        } catch (Exception e) {
          s = e.toString();
        }
        if (ae == null) {
          if (s == null) {
            s = "It's not listed in the white pages";
          }
        } else {
          URI uri = ae.getAddress();
          String path = uri.getPath();
          String node = (path == null ? null : path.substring(1));
          if (!destNode.getAddress().equals(node)) {
            s = "It's not a node agent "+ae;
          }
        }
        if (s != null) {
          // destination node is invalid!
          s =
            "Invalid destination node "+destNode+
            " for move of agent "+agentId+": "+
            s+", request uid is "+control.getOwnerUID();
          if (log.isErrorEnabled()) {
            log.error(s);
          }
          Throwable stack = new RuntimeException(s);
          control.setStatus(AgentControl.FAILURE, stack);
          blackboard.publishChange(control);
          return;
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
        } else if (ae.pendingAction != AgentEntry.NONE) {
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
            ae.pendingAction = AgentEntry.MOVE_DEPART;
            ae.control = control;
          }
        }
      }

      if (errorMsg != null) {
        if (log.isErrorEnabled()) {
          log.error(errorMsg);
        }
        Throwable stack = new RuntimeException(errorMsg);
        control.setStatus(AgentControl.FAILURE, stack);
        blackboard.publishChange(control);
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
        control.setStatus(AgentControl.MOVED, null);
        blackboard.publishChange(control);
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
            agent, control, null,
            id, moveTicket);

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

    } else if (abstractTicket instanceof TransferTicket) {

      if (!(isNode)) return;

      TransferTicket transferTicket = (TransferTicket) abstractTicket;

      MoveTicket moveTicket = transferTicket.getMoveTicket();

      MessageAddress destNode = moveTicket.getDestinationNode();
      if (destNode == null) {
        // not expected, since only remote controls
        // create transfers
        if (log.isErrorEnabled()) {
          log.error(
              "Unexpected agent-transfer "+control.getUID()+
              " added on node "+nodeId+
              " with null destination node, ticket: "+moveTicket);
        }
        return;
      } else if (!(nodeId.equals(destNode))) {
        if (nodeId.equals(control.getSource())) {
          // created by this plugin, ignore
          return;
        } else {
          // created by this plugin
          if (log.isErrorEnabled()) {
            log.error(
                "Invalid agent transfer with source "+
                control.getSource()+" to node "+destNode+
                " doesn't match this node "+nodeId);
          }
          return;
        }
      }

      MessageAddress id = moveTicket.getMobileAgent();

      // make sure agent is not registered, lock in arrival
      String errorMsg = null;
      synchronized (entries) {
        AgentEntry ae = (AgentEntry) entries.get(id);
        boolean isNew = false;
        if (ae == null) {
          isNew = true;
          ae = new AgentEntry(id);
          entries.put(id, ae);
        }
        if (ae.pendingAction != AgentEntry.NONE) {
          // agent is leaving this node?
          errorMsg = 
            "Unable to accept remote agent "+id+
            ", a move is already in progress: "+ae;
        } else if (ae.isRegistered) {
          // already moving or adding the agent?
          errorMsg = 
            "Unable to accept remote agent "+id+
            ", that agent is already on node "+nodeId+": "+ae;
        } else {
          ae.pendingAction = AgentEntry.MOVE_ARRIVAL;
          ae.control = control;
          if (log.isDebugEnabled()) {
            if (isNew) {
              log.debug(
                  "Created new entry for agent "+id+
                  " move arrival"+
                  " (oid: "+
                  System.identityHashCode(ae)+
                  "): "+ae);
            } else {
              log.debug(
                 "Updated entry for agent "+id+
                  " move arrival"+
                  " (oid: "+
                  System.identityHashCode(ae)+
                  "): "+ae+
                  ", description "+
                  objectCompare(ae.desc, ae.desc)+
                  ", agent "+
                  objectCompare(ae.agent, ae.agent));
            }
          }
        }
      }

      if (errorMsg != null) {
        if (log.isErrorEnabled()) {
          log.error(errorMsg);
        }
        Throwable stack = new RuntimeException(errorMsg);
        control.setStatus(AgentControl.FAILURE, stack);
        blackboard.publishChange(control);
        return;
      }

      StateTuple state = transferTicket.getStateTuple();

      // force GC of the agent state once transfer-ADD completes
      transferTicket.clearStateTuple();

      MobilitySupportImpl support = 
        new MobilitySupportImpl(
            null, null, control,
            id, moveTicket);

      AbstractHandler h =
        new ArrivalHandler(
            support, state);

      queue(id, h, support);
    } else {
      // ignore
    }
  }

  /** a control was changed. */
  protected void changedAgentControl(AgentControl control) {

    if (!(isNode)) return;

    AbstractTicket abstractTicket = control.getAbstractTicket();

    if (log.isDebugEnabled()) {
      log.debug("Observed change of "+control.getUID());
    }

    if (!(abstractTicket instanceof TransferTicket)) {
      return;
    }
    TransferTicket transferTicket = (TransferTicket) abstractTicket;

    MoveTicket moveTicket = transferTicket.getMoveTicket();

    MessageAddress id = moveTicket.getMobileAgent();

    MessageAddress origNode = moveTicket.getOriginNode();
    if ((origNode != null) &&
        (!(nodeId.equals(origNode)))) {
      if (origNode.equals(control.getSource())) {
        // ignore, changed by this plugin
        return;
      } else {
        if (log.isErrorEnabled()) {
          log.error(
              "Invalid change in transfer "+control.getUID()+
              ", intended for origin node "+origNode+
              ", not local node "+nodeId);
        }
        return;
      }
    }

    int status = control.getStatusCode();
    if (status == AgentEntry.NONE) {
      if (log.isDebugEnabled()) {
        log.debug(
            "Ignore change with no status for transfer "+
            control.getUID());
      }
      return;
    }

    boolean isNack = (status != AgentControl.MOVED);
    Throwable stack = 
      (isNack ? 
       control.getFailureStackTrace() :
       null);

    // force GC of the captured agent state
    //
    // this is important, otherwise the state will be
    // transfered again when we publish-remove the
    // transfer-control object.
    transferTicket.clearStateTuple();

    // remove the completed transfer
    //
    // this may complicate debugging, but it helps ensure
    // GC of these transfer-controls even if the original
    // move-control is never removed.  The other option is
    // to wait for the removal of the move-control.
    blackboard.publishRemove(control);

    // make sure agent is not registered, lock in arrival
    String errorMsg = null;
    Agent agent = null;
    synchronized (entries) {
      AgentEntry ae = (AgentEntry) entries.get(id);
      if (ae == null) {
        // no such control request
        errorMsg = 
          "Unknown agent "+id+" on node "+nodeId+
          ", so unable to process move "+
          (isNack ? "failure" : "success")+
          " response";
      } else if (ae.pendingAction != AgentEntry.MOVE_DEPART) {
        // agent is not moving, so we're not expecting an [n]ack
        errorMsg = 
          "Agent "+id+" is not moving on node "+nodeId+
          ", so unable to process move "+
          (isNack ? "failure" : "success")+
          " response";
      } else if (!(ae.isRegistered)) {
        // expecting the agent to stay registered during control
        // since unregister is in agent's "stop()".
        errorMsg = 
          "Agent "+id+" on node "+nodeId+
          " is no longer registered";
      } else {
        agent = ae.agent;
        ae.pendingAction = AgentEntry.MOVE_CONFIRM;
        ae.control = control;
      }
    }

    if (errorMsg != null) {
      if (log.isErrorEnabled()) {
        log.error(errorMsg, stack);
      }
      return;
    }

    // find the original "move" control
    UID moveControlUID = control.getOwnerUID();
    AgentControl moveControl = 
      findAgentControl(
          moveControlUID);
    if (moveControl == null) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Agent "+id+
            " control request "+moveControlUID+
            " for transfer "+control.getUID()+
            " not found in node "+nodeId+"'s blackboard, "+
            " will be unable to set the control status, "+
            " but will complete the control anyways");
      }
    }

    MobilitySupportImpl support = 
      new MobilitySupportImpl(
          agent, moveControl, control,
          id, moveTicket);

    AbstractHandler h;
    if (isNack) {
      h = new NackHandler(support, agent, stack);
    } else {
      h = new AckHandler(support, agent);
    }

    queue(id, h, support);
  }

  /** a control was removed. */
  protected void removedAgentControl(AgentControl control) {

    if (!(isNode)) return;

    AbstractTicket abstractTicket = control.getAbstractTicket();

    if (log.isDebugEnabled()) {
      log.debug("Observed removal of "+control.getUID());
    }

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
        } else {
          // must be moving
          if (log.isDebugEnabled()) {
            log.debug(
                "Registered agent "+id+" on node "+
                nodeId+", which already had an entry"+
                " (oid: "+
                System.identityHashCode(ae)+
                "): "+ae+
                ", the description "+
                objectCompare(ae.desc, desc)+
                " and agent "+
                objectCompare(ae.agent, agent));
          }
        }
      } else {
        ae = new AgentEntry(id);
        entries.put(id, ae);
        if (log.isDebugEnabled()) {
          log.debug(
              "Registered agent "+id+" on node "+
              nodeId+", which is a new entry"+
              " (oid: "+
              System.identityHashCode(ae)+
              "): "+ae+
              ", the description "+
              objectCompare(null, desc)+
              " and agent "+
              objectCompare(null, agent));
        }
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
        if (ae.pendingAction == AgentEntry.NONE) {
          // no longer needed
          entries.remove(id);
          if (log.isDebugEnabled()) {
            log.debug(
                "Unregistered agent "+id+
                " on node "+nodeId+
                ", removed entry"+
                " (oid: "+
                System.identityHashCode(ae)+
                "): "+ae);
          }
        } else {
          // agent is unloading as part of move,
          // keep the entry in case the move fails
          if (log.isDebugEnabled()) {
            log.debug(
                "Unregistered agent "+id+
                " on node "+nodeId+
                ", keeping the entry"+
                " (oid: "+
                System.identityHashCode(ae)+
                "): "+ae);
          }
        }
      }
    }
  }

  private static String objectCompare(Object a, Object b) {
    if (a == b) {
      return 
        "is identical ("+
        (a != null ?
         ("oid: "+
          System.identityHashCode(a)) : 
         "null")+
        ") "+
        a;
    }
    if (a != null && a.equals(b)) {
      return
        "is equivalent from prior "+
        (a != null ?
         ("(oid: "+
          System.identityHashCode(a)+
          ") "+
          a) :
         "null")+
        " to new "+
        "(oid: "+
        System.identityHashCode(b)+
        ") "+
        b;
    }
    return
        "has changed from prior "+
        (a != null ?
         ("(oid: "+
          System.identityHashCode(a)+
          ") "+
          a) :
         "null")+
        " to new "+
        (b != null ?
         ("(oid: "+
          System.identityHashCode(b)+
          ") "+
          b) :
         "null");
  }

  private interface Queueable extends Runnable {

    public boolean completeTransaction();

    public boolean beforeRun();

    public void run();

    public boolean afterRun();

    public boolean resumeTransaction();

  }

  private void queue(
      MessageAddress id,
      AbstractHandler h,
      MobilitySupportImpl support) {
    queue(id, h, support.pendingTuples);
  }

  private void queue(
      final MessageAddress id,
      final Runnable r,
      final List pendingTuples) {
    // ensure queue cleanup
    Runnable r2 = new Runnable() {
      public void run() {
        try {
          r.run();
        } finally {
          dequeue(id, r, pendingTuples);
        }
      }
      public String toString() {
        return r.toString();
      }
    };
    queue(r2);
  }

  private void dequeue(
      MessageAddress id,
      Runnable r2,
      final List pendingTuples) {
    if (r2 instanceof DispatchRemoteHandler) {
      // leave the entry, we're waiting for the response.
      if (log.isDebugEnabled()) {
        synchronized (entries) {
          AgentEntry ae = (AgentEntry) entries.get(id);
          log.debug(
              "Completed move action <dispatch> "+
              r2+
              " for agent "+id+
              " on node "+nodeId+
              ", keeping the move entry"+
              " (oid: "+
              System.identityHashCode(ae)+
              "): "+ae);
        }
      }
    } else {
      // remove moving flag
      synchronized (entries) {
        AgentEntry ae = (AgentEntry) entries.get(id);
        if (ae == null) {
          // aborted handler?
          if (log.isDebugEnabled()) {
            log.debug(
                "Completed move action "+
                r2+
                " for agent "+id+
                " on node "+nodeId+
                ", but the entry is null");
          }
        } else {
          ae.pendingAction = AgentEntry.NONE;
          if (!(ae.isRegistered)) {
            entries.remove(id);
            if (log.isDebugEnabled()) {
              log.debug(
                  "Completed move action "+
                  r2+
                  " for agent "+id+
                  " on node "+nodeId+
                  ", removed entry"+
                  " (oid: "+
                  System.identityHashCode(ae)+
                  "): "+ae);
            }
          } else {
            // keep in table for future moves
            if (log.isDebugEnabled()) {
              log.debug(
                  "Completed move action "+
                  r2+
                  " for agent "+id+
                  " on node "+nodeId+
                  ", keeping the entry "+
                  " (oid: "+
                  System.identityHashCode(ae)+
                  "): "+ae);
            }
          }
        }
      }
    }
    // queue pending blackboard operations
    if (!pendingTuples.isEmpty()) {
      Runnable r3 = new Runnable() {
        public void run() {
          for (Iterator iter = pendingTuples.iterator(); 
              iter.hasNext();
              ) {
            PendingTuple pt = (PendingTuple) iter.next();
            if (log.isDebugEnabled()) {
              log.debug("Blackboard "+pt);
            }
            Object obj = pt.obj;
            switch (pt.op) {
              case PendingTuple.ADD:
                blackboard.publishAdd(obj);
                break;
              case PendingTuple.CHANGE:
                blackboard.publishChange(obj);
                break;
              case PendingTuple.REMOVE:
                blackboard.publishRemove(obj);
                break;
            }
          }
        }
      };
      fireLater(r3);
    }
  }

  private class AgentEntry {

    /**
     * pendingAction constants.
     */
    public static final int NONE          = 0;
    // local agent add
    public static final int ADD           = 1;
    // local agent remove
    public static final int REMOVE        = 2;
    // sender-side agent is moving away
    public static final int MOVE_DEPART   = 3;
    // target-side agent is being added
    public static final int MOVE_ARRIVAL  = 4;
    // sender-side process the move response
    public static final int MOVE_CONFIRM  = 5;

    public final MessageAddress id;
    public ComponentDescription desc;
    public Agent agent;

    public boolean isRegistered;

    public int pendingAction = NONE;

    public AgentControl control;

    public AgentEntry(MessageAddress id) {
      this.id = id;
    }

    public String getPendingActionAsString() {
      switch (pendingAction) {
        case NONE:         return "none";
        case ADD:          return "add";
        case REMOVE:       return "remove";
        case MOVE_DEPART:  return "move_depart";
        case MOVE_ARRIVAL: return "move_arrival";
        case MOVE_CONFIRM: return "move_confirm";
        default:           return "?";
      }
    }

    public String toString() {
      return 
        "control request for agent "+id+
        ", state is <"+
        (isRegistered ? "" :"not ")+
        "registered + "+
        getPendingActionAsString()+
        ">"+
        ((control != null) ? 
         (", with ticket "+ control.getAbstractTicket()) :
         "");
    }
  }

  private static class PendingTuple {
    public static final int ADD    = 0;
    public static final int CHANGE = 1;
    public static final int REMOVE = 2;
    public final Object obj;
    public final int op;
    public PendingTuple(int op, Object obj) {
      this.op = op;
      this.obj = obj;
    }
    public String toString() {
      return 
        "queued "+
        ((op == ADD) ? "add" :
         (op == CHANGE) ? "change" :
         (op == REMOVE) ? "remove" :
         "?")+
        " of object "+
        ((obj instanceof UniqueObject) ? 
         ("with uid "+(((UniqueObject) obj).getUID())) :
         (obj != null) ? obj.toString() :
         "null");
    }
  }

  private class MobilitySupportImpl 
    extends AbstractMobilitySupport {

      private final List pendingTuples = new ArrayList(3);

      private Agent agent;
      private AgentControl moveControl;
      private AgentControl transferControl;

      public MobilitySupportImpl(
          Agent agent,
          AgentControl moveControl,
          AgentControl transferControl,
          MessageAddress id,
          MoveTicket moveTicket) {
        super(
            id, 
            RootMobilityPlugin.this.nodeId, 
            moveTicket, 
            RootMobilityPlugin.this.log);
        this.agent = agent;
        this.moveControl = moveControl;
        this.transferControl = transferControl;
      } 

      public void onDispatch() {
        MessageAddress destNode = moveTicket.getDestinationNode();
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
        if (moveControl != null) {
          moveControl.setStatus(AgentControl.MOVED, null);
          publishChangeLater(moveControl);
        } else {
          if (RootMobilityPlugin.this.log.isWarnEnabled()) {
            RootMobilityPlugin.this.log.warn(
                "Unable to set move status for transfer "+
                ((transferControl != null) ? 
                 transferControl.getUID().toString() : 
                 "<unknown>"));
          }
        }
      }

      public void onFailure(Throwable throwable) {
        moveControl.setStatus(AgentControl.FAILURE, throwable);
        publishChangeLater(moveControl);
      }

      public void onRemoval() {
      }

      public void setPendingModel(GenericStateModel model) {
      }

      public GenericStateModel takePendingModel() {
        return null;
      }

      public void sendTransfer(StateTuple tuple) {
        TransferTicket transferTicket =
          new TransferTicket(
              moveTicket,
              tuple);
        AgentControl newTC = 
          createAgentControl(
              moveControl.getUID(),
              moveTicket.getDestinationNode(),
              transferTicket);
        transferControl = newTC;
        publishAddLater(newTC);
      }

      public void sendAck() {
        transferControl.setStatus(AgentControl.MOVED, null);
        publishChangeLater(transferControl);
      }

      public void sendNack(Throwable throwable) {
        transferControl.setStatus(AgentControl.FAILURE, throwable);
        publishChangeLater(transferControl);
      }

      public void addAgent(StateTuple tuple) {
        agentContainer.addAgent(id, tuple);
      }

      public void removeAgent() {
        agentContainer.removeAgent(id);
      }

      private void publishAddLater(Object o) {
        addPendingTuple(PendingTuple.ADD, o);
      }

      private void publishChangeLater(Object o) {
        addPendingTuple(PendingTuple.CHANGE, o);
      }

      private void publishRemoveLater(Object o) {
        addPendingTuple(PendingTuple.REMOVE, o);
      }

      private void addPendingTuple(int op, Object o) {
        addPendingTuple(new PendingTuple(op, o));
      }

      private void addPendingTuple(PendingTuple pt) {
        if (pt == null) {
          throw new IllegalArgumentException("null tuple");
        }
        pendingTuples.add(pt);
      }
    }
  
  private class AddAgentRunner implements Runnable {

    public final List pendingTuples = new ArrayList(1);

    private final MessageAddress id;
    private final AgentControl control;
    private final StateTuple tuple;

    public AddAgentRunner(
        MessageAddress id, 
        AgentControl control,
        StateTuple tuple) {
      this.id = id;
      this.control = control;
      this.tuple = tuple;
    }

    public void run() {

      // add into this node
      if (log.isInfoEnabled()) {
        log.info("Add agent "+id+" to node "+nodeId);
      }

      int resultState;
      Throwable resultStack = null;
      try {

        agentContainer.addAgent(id, tuple);

        // success!
        resultState = AgentControl.CREATED;

        if (log.isInfoEnabled()) {
          log.info("Added agent "+id+" to node "+nodeId);
        }

      } catch (Exception e) {
        // either already exists or unable to add
        //
        // HACK: check the exception message
        String msg = e.getMessage();
        if (msg != null && msg.indexOf(" already exists") > 0) {
          // already exists
          resultState = AgentControl.ALREADY_EXISTS;
          if (log.isErrorEnabled()) {
            log.error(
                "Agent " + id + " already exists on node "+nodeId);
          }
        } else {
          // couldn't add
          resultState = AgentControl.FAILURE;
          resultStack = e;
          if (log.isErrorEnabled()) {
            log.error("Unable to add agent " + id, e);
          }
        }
      }

      // set our response state
      control.setStatus(resultState, resultStack);

      // publish-change later
      PendingTuple pt = new PendingTuple(PendingTuple.CHANGE, control);
      pendingTuples.add(pt);
    }
  }

  private class RemoveAgentRunner implements Runnable {

    public final List pendingTuples = new ArrayList(1);

    private final MessageAddress id;
    private final AgentControl control;

    public RemoveAgentRunner(
        MessageAddress id, 
        AgentControl control) {
      this.id = id;
      this.control = control;
    }

    public void run() {

      // remove agent from this node
      if (log.isInfoEnabled()) {
        log.info("Remove agent "+id+" from node "+nodeId);
      }

      int resultState;
      Throwable resultStack = null;
      try {
        agentContainer.removeAgent(id);
        // success!
        resultState = AgentControl.REMOVED;
        if (log.isInfoEnabled()) {
          log.info("Removed agent "+id+" from node "+nodeId);
        }
      } catch (Exception e) {
        // either already removed or unable to remove
        //
        // HACK: check the exception message
        String msg = e.getMessage();
        if (msg != null && msg.indexOf(" is not loaded") > 0) {
          // already exists
          resultState = AgentControl.DOES_NOT_EXIST;
          if (log.isErrorEnabled()) {
            log.error("Agent " + id + " is not on node "+nodeId);
          }
        } else {
          // couldn't add
          resultState = AgentControl.FAILURE;
          resultStack = e;
          if (log.isErrorEnabled()) {
            log.error("Unable to remove agent " + id, e);
          }
        }
      }

      // set our response state
      control.setStatus(resultState, resultStack);

      // publish-change later
      PendingTuple pt = new PendingTuple(PendingTuple.CHANGE, control);
      pendingTuples.add(pt);
    }
  }
}
