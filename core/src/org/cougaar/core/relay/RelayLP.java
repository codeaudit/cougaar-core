/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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


package org.cougaar.core.relay;

import org.cougaar.core.mts.Message;
import org.cougaar.core.blackboard.LogPlanLogicProvider;
import org.cougaar.core.blackboard.EnvelopeLogicProvider;
import org.cougaar.core.blackboard.MessageLogicProvider;
import org.cougaar.core.blackboard.LogPlan;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.core.agent.RestartLogicProvider;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Collection;
import java.util.Iterator;

/** LP is a "LogPlan Logic Provider":
  *
  * it provides the logic to capture
  * PlanElements that are Transferable and send TransferableAssignment 
  * directives to the proper remote cluster.
  *
  **/

public class RelayLP extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, MessageLogicProvider, RestartLogicProvider
{
  private Relay.Token token = new Relay.Token();
  private ClusterIdentifier self;

  public RelayLP(LogPlan logplan, ClusterServesLogicProvider cluster) {
    super(logplan, cluster);
    self = cluster.getClusterIdentifier();
  }

  // EnvelopeLogicProvider implementation
  /**
   * Sends the Content of the Relay to the targets of the Relay.
   * @param Object Envelopetuple, where tuple.object == Relay.Source
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (obj instanceof Relay.Source) {
      Relay.Source ts = (Relay.Source) obj;
      for (Iterator i = ts.getTargets().iterator(); i.hasNext(); ) {
        ClusterIdentifier target = (ClusterIdentifier) i.next();
        if (target.equals(self)) continue; // Never send to self
        if (o.isAdd()) {
          sendAdd(ts, target);
        } else if (o.isChange()) {
          sendChange(ts, target, changes);
        } else if (o.isRemove()) {
          sendRemove(ts, target);
        }
      }
      return;
    }
    if (obj instanceof Relay.Target) {
      Relay.Target tt = (Relay.Target) obj;
      ClusterIdentifier source = tt.getSource();
      if (source == null) return; // No source
      if (source.equals(self)) return; // Never send to self
      if (o.isAdd()) {
        sendResponse(tt, source); // There may be a response due to arrival
        return;
      }
      if (o.isChange()) {
        sendResponse(tt, source);
        return;
      }
      if (o.isRemove()) {
        return;                 // This would be our own remove
      }
    }
  }

  private void sendAdd(Relay.Source ts, ClusterIdentifier target) {
    RelayDirective.Add dir = new RelayDirective.Add(ts.getUID(), ts.getContent());
    dir.setSource(self);
    dir.setDestination(target);
    logplan.sendDirective(dir);
  }

  private void sendChange(Relay.Source ts, ClusterIdentifier target, Collection changes) {
    RelayDirective.Change dir =
      new RelayDirective.Change(ts.getUID(), ts.getContent(), changes);
    dir.setSource(self);
    dir.setDestination(target);
    logplan.sendDirective(dir);
  }

  private void sendRemove(Relay.Source ts, ClusterIdentifier target) {
    RelayDirective.Remove dir = new RelayDirective.Remove(ts.getUID());
    dir.setSource(self);
    dir.setDestination(target);
    logplan.sendDirective(dir);
  }

  private void sendResponse(Relay.Target tt, ClusterIdentifier source) {
    Relay.Response tr = tt.getResponse();
    if (tr == null) return;     // No response wanted
    sendResponse(tt, source, tr);
  }

  private void sendResponse(Relay.Target tt, ClusterIdentifier source, Relay.Response tr) {
    RelayDirective.Response dir = new RelayDirective.Response(tt.getUID(), tr);
    dir.setSource(self);
    dir.setDestination(source);
    logplan.sendDirective(dir);
  }

  private void sendVerification(Relay.Target tt, ClusterIdentifier source) {
    Relay.Response tr = tt.getResponse();
    sendResponse(tt, source, tr); // Send even if null response
  }

  // MessageLogicProvider implementation

  public void execute(Directive dir, Collection changes) {
    if (dir instanceof RelayDirective) { // Quick test for one of ours
      if (dir instanceof RelayDirective.Change) {
        receiveChange((RelayDirective.Change) dir, changes);
        return;
      }
      if (dir instanceof RelayDirective.Add) {
        receiveAdd((RelayDirective.Add) dir);
        return;
      }
      if (dir instanceof RelayDirective.Remove) {
        receiveRemove((RelayDirective.Remove) dir);
        return;
      }
      if (dir instanceof RelayDirective.Response) {
        receiveResponse((RelayDirective.Response) dir);
        return;
      }
    }
  }

  private void receiveAdd(RelayDirective.Add dir) {
    Relay.Target tt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (tt == null) {
      tt = dir.getContent().create(dir.getUID(), dir.getSource(), token);
      logplan.add(tt);
    } else {
      // Unusual. Treat as change
      if (tt.updateContent(dir.getContent(), token)) logplan.change(tt, null);
    }
  }

  private void receiveChange(RelayDirective.Change dir, Collection changes) {
    Relay.Target tt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (tt == null) {
      // Unusual. Treat as add.
      tt = dir.getContent().create(dir.getUID(), dir.getSource(), token);
      logplan.change(tt, changes);
    } else {
      if (tt.updateContent(dir.getContent(), token)) logplan.change(tt, changes);
    }
  }

  private void receiveRemove(RelayDirective.Remove dir) {
    Relay.Target tt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (tt == null) {
      // Unusual. Ignore.
    } else {
      logplan.remove(tt);
    }
  }

  private void receiveResponse(RelayDirective.Response dir) {
    Relay.Source ts = (Relay.Source) logplan.findUniqueObject(dir.getUID());
    ClusterIdentifier target = dir.getSource();
    if (ts == null) {
      // No longer part of our logplan. Rescind it.
      sendRemove(ts, target);
    } else {
      Relay.Response resp = dir.getResponse();
      if (resp != null) ts.setResponse(target, resp);
    }
  }

  // RestartLogicProvider implementation

  /**
   * Cluster restart handler. Resend all our Relay.Source again and
   * send verification directives for all our Relay.Targets.
   **/
  public void restart(final ClusterIdentifier cid) {
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof Relay;
      }
    };
    Enumeration enum = logplan.searchBlackboard(pred);
    while (enum.hasMoreElements()) {
      Relay t = (Relay) enum.nextElement();
      if (t instanceof Relay.Source) {
        resend((Relay.Source) t, cid);
      }
      if (t instanceof Relay.Target) {
        verify((Relay.Target) t, cid);
      }
    }
  }

  private void resend(Relay.Source ts, ClusterIdentifier cid) {
    for (Iterator i = ts.getTargets().iterator(); i.hasNext(); ) {
      ClusterIdentifier target = (ClusterIdentifier) i.next();
      if (target.equals(self)) continue; // Don't send to ourself
      if (cid == null || target.equals(cid)) {
        sendAdd(ts, target);
      }
    }
  }

  private void verify(Relay.Target tt, ClusterIdentifier cid) {
    ClusterIdentifier source = tt.getSource();
    if (source.equals(self)) return; // Don't send to ourself
    if (cid == null || source.equals(cid)) {
      sendResponse(tt, source);
    }
  }
}
