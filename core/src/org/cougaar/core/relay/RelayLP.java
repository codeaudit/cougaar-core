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

import java.io.Serializable;
import java.util.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.agent.RestartLogicProvider;
import org.cougaar.core.blackboard.AnonymousChangeReport;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.EnvelopeLogicProvider;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.LogPlanLogicProvider;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.blackboard.MessageLogicProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

import org.cougaar.planning.ldm.plan.Directive;

import org.cougaar.util.UnaryPredicate;

/**
 * Logic provider to transmit and update Relay objects.
 *
 * @see Relay
 */
public class RelayLP extends LogPlanLogicProvider
  implements EnvelopeLogicProvider, MessageLogicProvider, RestartLogicProvider
{
  private Relay.Token token = new Relay.Token();
  private MessageAddress self;

  public RelayLP(
      LogPlanServesLogicProvider logplan, 
      ClusterServesLogicProvider cluster) {
    super(logplan, cluster);
    self = cluster.getClusterIdentifier();
  }

  // EnvelopeLogicProvider implementation
  /**
   * Sends the Content of the Relay to the targets of the Relay.
   * @param o an Envelopetuple where the tuple.object is
   *    a Relay.Source or Relay.Target
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (obj instanceof Relay) {
      MessageAddress source = ((Relay) obj).getSource();
      if ((source == null) ||
          (self.equals(source))) {
        Relay.Source rs = (Relay.Source) obj;
        if (o.isAdd()) {
          localAdd(rs);
        } else if (o.isChange()) {
          localChange(rs, changes);
        } else if (o.isRemove()) {
          localRemove(rs);
        }
      } else {
        Relay.Target rt = (Relay.Target) obj;
        if (o.isChange()) {
          localResponse(rt, source, changes);
        } else {
          // Ignore our own add & remove
        }
      }
    }
  }

  private void localAdd(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets.isEmpty()) return; // No targets
    Object content = rs.getContent();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target.equals(self)) {
        // Never send to self.  Likely an error.
        continue; 
      }
      sendAdd(rs, target, content);
    }
  }

  private void localChange(Relay.Source rs, Collection changes) {
    Set targets = rs.getTargets();
    if (targets.isEmpty()) return; // No targets
    // cancel if (changes == ResponseReport.LIST)
    if ((changes == ResponseReport.LIST) ||
        ((changes != AnonymousChangeReport.LIST) &&
         (changes.contains(ResponseReport.INSTANCE)))) {
      return;
    }
    // FIXME check for targets-change-report:
    //   calculate set differences
    //   for added targets: sendAdd
    //   for removed targets: sendRemove
    // add ContentReport to changes
    Collection c;
    if ((changes == ContentReport.LIST) ||
        (changes == AnonymousChangeReport.LIST) ||
        (changes == null)) {
      c = ContentReport.LIST;
    } else {
      c = new ArrayList(changes.size() + 1);
      c.addAll(changes);
      c.add(ContentReport.INSTANCE);
    }
    Object content = rs.getContent();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target.equals(self)) {
        // Never send to self.  Likely an error.
        continue; 
      }
      sendChange(rs, target, content, c);
    }
  }

  private void localRemove(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets.isEmpty()) return; // No targets
    UID uid =  rs.getUID();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target.equals(self)) {
        // Never send to self.  Likely an error.
        continue; 
      }
      sendRemove(uid, target);
    }
  }

  private void localResponse(Relay.Target rt, MessageAddress source, Collection changes) {
    // cancel if (changes == ContentReport.LIST)
    if ((changes == ContentReport.LIST) ||
        ((changes != AnonymousChangeReport.LIST) &&
         (changes.contains(ContentReport.INSTANCE)))) {
      return;
    }
    Object resp = rt.getResponse();
    // cancel if response is null
    if (resp == null) {
      return;
    }
    Collection c;
    if ((changes == ResponseReport.LIST) ||
        (changes == AnonymousChangeReport.LIST) ||
        (changes == null)) {
      c = ResponseReport.LIST;
    } else {
      c = new ArrayList(changes.size() + 1);
      c.addAll(changes);
      c.add(ResponseReport.INSTANCE);
    }
    sendResponse(rt, source, resp, c);
  }

  private void sendAdd(Relay.Source rs, MessageAddress target, Object content) {
    RelayDirective.Add dir = 
      new RelayDirective.Add(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource((ClusterIdentifier) self);
    dir.setDestination((ClusterIdentifier) target);
    logplan.sendDirective(dir);
  }

  private void sendChange(
      Relay.Source rs, MessageAddress target, Object content, Collection c) {
    // assert (c.contains(ContentReport.INSTANCE))
    // assert (!(c.contains(ResponseReport.INSTANCE)))
    RelayDirective.Change dir =
      new RelayDirective.Change(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource((ClusterIdentifier) self);
    dir.setDestination((ClusterIdentifier) target);
    logplan.sendDirective(dir, c);
  }

  private void sendRemove(UID uid, MessageAddress target) {
    RelayDirective.Remove dir = new RelayDirective.Remove(uid);
    dir.setSource((ClusterIdentifier) self);
    dir.setDestination((ClusterIdentifier) target);
    logplan.sendDirective(dir);
  }

  private void sendResponse(
      Relay.Target rt, MessageAddress source, Object resp, Collection c) {
    // assert (c.contains(ResponseReport.INSTANCE))
    // assert (!(c.contains(ContentReport.INSTANCE)))
    RelayDirective.Response dir = new RelayDirective.Response(rt.getUID(), resp);
    dir.setSource((ClusterIdentifier) self);
    dir.setDestination((ClusterIdentifier) source);
    logplan.sendDirective(dir, c);
  }

  private void sendVerification(Relay.Target rt, MessageAddress source) {
    Object resp = rt.getResponse();
    // Send even if null response
    sendResponse(rt, source, resp, ResponseReport.LIST);
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
        receiveResponse((RelayDirective.Response) dir, changes);
        return;
      }
    }
  }

  private void receiveAdd(RelayDirective.Add dir) {
    Relay.Target rt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      Object cont = dir.getContent();
      Relay.TargetFactory tf = dir.getTargetFactory();
      if (tf == null) {
        rt = (Relay.Target) cont;
      } else {
        rt = tf.create(dir.getUID(), dir.getSource(), cont, token);
      }
      logplan.add(rt);
      // Check for immediate response due to arrival
      Object resp = rt.getResponse();
      if (resp != null) {
        sendResponse(rt, dir.getSource(), resp, ResponseReport.LIST);
      }
    } else {
      // Unusual. Treat as change
      if (rt.updateContent(dir.getContent(), token)) {
        logplan.change(rt, ResponseReport.LIST);
      }
    }
  }

  private void receiveChange(RelayDirective.Change dir, Collection changes) {
    // assert (changes.contains(ContentReport.INSTANCE))
    // assert (!(changes.contains(ResponseReport.INSTANCE)))
    Relay.Target rt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Treat as add.
      Object cont = dir.getContent();
      Relay.TargetFactory tf =  dir.getTargetFactory();
      if (tf == null) {
        rt = (Relay.Target) cont;
      } else {
        rt = tf.create(dir.getUID(), dir.getSource(), cont, token);
      }
      logplan.add(rt);
    } else {
      if (rt.updateContent(dir.getContent(), token)) {
        logplan.change(rt, changes);
      }
    }
  }

  private void receiveRemove(RelayDirective.Remove dir) {
    Relay.Target rt = (Relay.Target) logplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Ignore.
    } else {
      logplan.remove(rt);
    }
  }

  private void receiveResponse(RelayDirective.Response dir, Collection changes) {
    // assert (changes.contains(ResponseReport.INSTANCE))
    // assert (!(changes.contains(ContentReport.INSTANCE)))
    Relay.Source rs = (Relay.Source) logplan.findUniqueObject(dir.getUID());
    MessageAddress target = dir.getSource();
    if (rs == null) {
      // No longer part of our logplan. Rescind it.
      sendRemove(dir.getUID(), target);
    } else {
      Object resp = dir.getResponse();
      if (resp != null) {
        if (rs.updateResponse(target, resp)) {
          logplan.change(rs, changes);
        }
      }
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
    Enumeration en = logplan.searchBlackboard(pred);
    while (en.hasMoreElements()) {
      Relay r = (Relay) en.nextElement();
      MessageAddress source = r.getSource();
      if ((source == null) ||
          (self.equals(source))) {
        Relay.Source rs = (Relay.Source) r;
        resend(rs, cid);
      } else if (source == null) {
        // No source?
      } else {
        Relay.Target rt = (Relay.Target) r;
        verify(rt, cid);
      }
    }
  }

  private void resend(Relay.Source rs, MessageAddress t) {
    Set targets = rs.getTargets();
    if (targets.isEmpty()) return;
    Object content = rs.getContent();
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target.equals(self)) {
        // Don't send to ourself.  Likely an error.
      } else { 
        if (t == null || target.equals(t)) {
          sendAdd(rs, target, content);
        }
      }
    }
  }

  private void verify(Relay.Target rt, MessageAddress s) {
    MessageAddress source = rt.getSource();
    if (source.equals(self)) {
      // Don't send to ourself.  Likely an error.
    } else {
      if (s == null || source.equals(s)) {
        sendVerification(rt, source);
      }
    }
  }


  /** 
   * ChangeReport for this LP to identify it's own content changes.
   */
  private static final class ContentReport implements ChangeReport {
    public static final ContentReport INSTANCE = new ContentReport();
    public static final List LIST = new ContentReportList();
    private ContentReport() { }
    private Object readResolve() { return INSTANCE; }
    public String toString() { return "relay-content-report"; }
    static final long serialVersionUID = 9091843781928322223L;
    // singleton LIST with singleton-friendly "readResolve()":
    private static final class ContentReportList extends AbstractList
      implements RandomAccess, Serializable {
        private ContentReportList() { }
        public int size() {return 1;}
        public boolean contains(Object obj) {return (obj == INSTANCE);}
        public Object get(int index) {
          if (index != 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
          return INSTANCE;
        }
        private Object readResolve() { return LIST; }
        static final long serialVersionUID = 4912831092837819234L;
      }
  }

  /** 
   * ChangeReport for this LP to identify it's own response changes.
   */
  private static final class ResponseReport implements ChangeReport {
    public static final ResponseReport INSTANCE = new ResponseReport();
    public static final List LIST = new ResponseReportList();
    private ResponseReport() { }
    private Object readResolve() { return INSTANCE; }
    public String toString() { return "relay-response-report"; }
    static final long serialVersionUID = 890230981268902124L;
    // singleton LIST with singleton-friendly "readResolve()":
    private static final class ResponseReportList extends AbstractList
      implements RandomAccess, Serializable {
        private ResponseReportList() { }
        public int size() {return 1;}
        public boolean contains(Object obj) {return (obj == INSTANCE);}
        public Object get(int index) {
          if (index != 0)
            throw new IndexOutOfBoundsException("Index: "+index+", Size: 1");
          return INSTANCE;
        }
        private Object readResolve() { return LIST; }
        static final long serialVersionUID = 1029836829182738922L;
      }
  }

}
