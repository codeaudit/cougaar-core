/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.relay;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.blackboard.ABATranslation;
import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.domain.ABAChangeLogicProvider;
import org.cougaar.core.domain.EnvelopeLogicProvider;
import org.cougaar.core.domain.LogicProvider;
import org.cougaar.core.domain.MessageLogicProvider;
import org.cougaar.core.domain.RestartLogicProvider;
import org.cougaar.core.domain.RootPlan;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.LoggerFactory;

/**
 * Logic provider to transmit and update Relay objects.
 *
 * @see Relay
 */
public class RelayLP
implements LogicProvider, EnvelopeLogicProvider, MessageLogicProvider, RestartLogicProvider, ABAChangeLogicProvider
{
  private final RootPlan rootplan;
  private final MessageAddress self;
  private final Relay.Token token;

  private final Logger logger = LoggerFactory.getInstance().createLogger(getClass());

  public RelayLP(
      RootPlan rootplan, 
      MessageAddress self) {
    this.rootplan = rootplan;
    this.self = self;
    token = TokenImpl.getToken(self);
  }

  public void init() {
  }

  // EnvelopeLogicProvider implementation
  /**
   * Sends the Content of Relay sources to the their targets and sends
   * target responses back to the source.
   * @param o an EnvelopeTuple where the tuple.object is
   *    a Relay.Source or Relay.Target
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (obj instanceof Relay) { // Quick test for Target or Source
      if (changes != null && changes.contains(MarkerReport.INSTANCE)) {
        return;                 // Ignore changes containing our MarkerReport
      }
      if (obj instanceof Relay.Target) {
        Relay.Target rt = (Relay.Target) obj;
        if (o.isChange()) {
          localResponse(rt, changes); // Only changes are significant at a Target
        }
      }
      if (obj instanceof Relay.Source) {
        Relay.Source rs = (Relay.Source) obj;
        if (o.isAdd()) {
          localAdd(rs);
        } else if (o.isChange()) {
          localChange(rs, changes);
        } else if (o.isRemove()) {
          localRemove(rs);
        }
      }
    }
  }

  private void localAdd(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets == null) return;
    if (targets.isEmpty()) return; // No targets
    localAdd(rs, targets);
  }

  private void localAdd(Relay.Source rs, Set targets) {
    boolean gotContent = false;
    Object content = null;
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.equals(self)) {
        // Never send to self.  Likely an error.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
        sendAdd(rs, target, content);
      }
    }
  }

  /**
   * Handle a change to this source. We need to send the new content
   * to the targets.
   **/
  private void localChange(Relay.Source rs, Collection changes) {
    Set targets = rs.getTargets();
    Collection oldTargets = null;
    if (changes != null) {
      for (Iterator i = changes.iterator(); i.hasNext(); ) {
        Object o = i.next();
        if (o instanceof RelayChangeReport) {
          if (oldTargets == null) {
            RelayChangeReport rcr = (RelayChangeReport) o;
            oldTargets = rcr.getOldTargets();
          }
          i.remove();
        }
      }
    }
    if (oldTargets != null) {
      if (targets != null) oldTargets.removeAll(targets);
      UID uid = rs.getUID();
      for (Iterator i = oldTargets.iterator(); i.hasNext(); ) {
        MessageAddress target = (MessageAddress) i.next();
        sendRemove(uid, target);
      }
    }
    if (targets == null || targets.isEmpty()) {
      return; // No targets
    }

    // FIXME check for targets-change-report:
    //   calculate set differences
    //   for added targets: sendAdd
    //   for removed targets: sendRemove
    // add ContentReport to changes
    boolean gotContent = false;
    Object content = null;
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.equals(self)) {
        // Never send to self.  Likely an error.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
        sendChange(rs, target, content, changes);
      }
    }
  }

  private void localRemove(Relay.Source rs) {
    Set targets = rs.getTargets();
    if (targets == null) return;
    if (targets.isEmpty()) return; // No targets
    localRemove(rs.getUID(), targets);
  }

  private void localRemove(UID uid, Set targets) {
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.equals(self)) {
        // Never send to self.  Likely an error.
      } else {
        sendRemove(uid, target);
      }
    }
  }

  /**
   * Handle a change to this target. We need to send the new response
   * to the source
   **/
  private void localResponse(Relay.Target rt, Collection changes) {
    MessageAddress source = rt.getSource();
    if (source == null) return; // No source
    if (self.equals(source)) return; // BOGUS source must be elsewhere. Ignore.

    Object resp = rt.getResponse();
    // cancel if response is null
    if (resp == null) return;

    sendResponse(rt, source, resp, changes);
  }

  private void sendAdd(Relay.Source rs, MessageAddress target, Object content) {
    RelayDirective.Add dir = 
      new RelayDirective.Add(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir);
  }

  private void sendChange(
      Relay.Source rs, MessageAddress target, Object content, Collection c) {
    RelayDirective.Change dir =
      new RelayDirective.Change(rs.getUID(), content, rs.getTargetFactory());
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir, c);
  }

  private void sendRemove(UID uid, MessageAddress target) {
    RelayDirective.Remove dir = new RelayDirective.Remove(uid);
    dir.setSource(self);
    dir.setDestination(target);
    rootplan.sendDirective(dir);
  }

  private void sendResponse(
      Relay.Target rt, MessageAddress source, Object resp, Collection c) {
    RelayDirective.Response dir = new RelayDirective.Response(rt.getUID(), resp);
    dir.setSource(self);
    dir.setDestination(source);
    rootplan.sendDirective(dir, c);
  }

  private void sendVerification(Relay.Target rt, MessageAddress source) {
    Object resp = rt.getResponse();
    // Send even if null response
    sendResponse(rt, source, resp, Collections.EMPTY_SET);
  }

  // MessageLogicProvider implementation

  public void execute(Directive dir, Collection changes) {
    if (dir instanceof RelayDirective) { // Quick test for one of ours
      if (self.equals(dir.getSource())) return;
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

  private void addTarget(Relay.TargetFactory tf, Object cont, RelayDirective dir) {
    Relay.Target rt;
    if (tf != null) {
      rt = tf.create(dir.getUID(), dir.getSource(), cont, token);
    } else if (cont instanceof Relay.Target) {
      rt = (Relay.Target) cont;
    } else {
      // ERROR cannot create target
      return;
    }
    if (rt == null) return;     // Target should not exist here
    rootplan.add(rt);
    // Check for immediate response due to arrival
    Object resp = rt.getResponse();
    if (resp != null) {
      sendResponse(rt, dir.getSource(), resp, Collections.EMPTY_SET);
    }
  }

  private void changeTarget(Relay.Target rt, Object cont, Collection changes) {
    int flags = rt.updateContent(cont, token);
    if ((flags & Relay.CONTENT_CHANGE) != 0) {
      Collection c;
      if (changes == null) {
        c = Collections.singleton(MarkerReport.INSTANCE);
      } else {
        c = new ArrayList(changes);
        c.add(MarkerReport.INSTANCE);
      }
      rootplan.change(rt, c);
      if (rt instanceof Relay.Source) localChange((Relay.Source) rt, changes);
    }
    if ((flags & Relay.RESPONSE_CHANGE) != 0) {
      localResponse(rt, Collections.EMPTY_SET);
    }
  }

  private void receiveAdd(RelayDirective.Add dir) {
    Relay.Target rt = (Relay.Target) rootplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      addTarget(dir.getTargetFactory(), dir.getContent(), dir);
    } else {
      // Unusual. Treat as change
      changeTarget(rt, dir.getContent(), Collections.EMPTY_SET);
    }
  }

  private void receiveChange(RelayDirective.Change dir, Collection changes) {
    Relay.Target rt = (Relay.Target) rootplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Treat as add.
      addTarget(dir.getTargetFactory(), dir.getContent(), dir);
    } else {
      changeTarget(rt, dir.getContent(), changes);
    }
  }

  private void receiveRemove(RelayDirective.Remove dir) {
    Relay.Target rt = (Relay.Target) rootplan.findUniqueObject(dir.getUID());
    if (rt == null) {
      // Unusual. Ignore.
    } else {
      rootplan.remove(rt);
    }
  }

  private void receiveResponse(RelayDirective.Response dir, Collection changes) {
    UniqueObject uo = rootplan.findUniqueObject(dir.getUID());
    MessageAddress target = dir.getSource();
    if (! (uo instanceof Relay.Source) && uo != null) {
      // This is not legitimate. We'll get a ClassCastException below
      // if we're not careful
      logger.error(self + ": receiveResponse got non Relay.Source (Bug 3202?). Got: " + uo + " from the Response[" + dir.getUID() + "] with source " + target + " and dest " + dir.getDestination() + ", response " + dir.getResponse(), new Throwable()); 
      return;
    }
    Relay.Source rs = (Relay.Source) uo;
    //    Relay.Source rs = (Relay.Source) rootplan.findUniqueObject(dir.getUID());
    if (rs == null) {
      // No longer part of our blackboard. Rescind it.
      if (logger.isInfoEnabled())
	logger.info(self + ": receiveResponse got NULL Relay.Source from the Response[" + dir.getUID() + "] with source " + target + " and dest " + dir.getDestination() + ", response " + dir.getResponse()); 
      
      sendRemove(dir.getUID(), target);
    } else {
      Object resp = dir.getResponse();
      if (resp != null) {
        int flags = rs.updateResponse(target, resp);
        if ((flags & Relay.RESPONSE_CHANGE) != 0) {
          Collection c;
          if (changes == null) {
            c = new ArrayList(1);
          } else {
            c = new ArrayList(changes);
          }
          c.add(MarkerReport.INSTANCE);
          rootplan.change(rs, c);
          if (rs instanceof Relay.Target) localResponse((Relay.Target) rs, changes);
        }
        if ((flags & Relay.CONTENT_CHANGE) != 0) {
          localChange(rs, Collections.EMPTY_SET);
        }
      }
    }
  }

  // RestartLogicProvider implementation

  /**
   * Cluster restart handler. Resend all our Relay.Source again and
   * send verification directives for all our Relay.Targets.
   **/
  public void restart(final MessageAddress cid) {
    if (logger.isInfoEnabled()) {
      logger.info(
        self+": Reconcile with "+
        (cid==null?"all agents":cid.toString()));
    }
    UnaryPredicate pred = new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof Relay;
      }
    };
    Enumeration en = rootplan.searchBlackboard(pred);
    while (en.hasMoreElements()) {
      Relay r = (Relay) en.nextElement();
      if (r instanceof Relay.Source) {
        Relay.Source rs = (Relay.Source) r;
        resend(rs, cid);
      }
      if (r instanceof Relay.Target) {
        Relay.Target rt = (Relay.Target) r;
        verify(rt, cid);
      }
    }
    if (logger.isInfoEnabled()) {
      logger.info(self+": Reconciled");
    }
  }

  private void resend(Relay.Source rs, MessageAddress t) {
    Set targets = rs.getTargets();
    if (targets == null) return; // Not really a source
    if (targets.isEmpty()) return;
    boolean gotContent = false;
    Object content = null;
    for (Iterator i = targets.iterator(); i.hasNext(); ) {
      MessageAddress target = (MessageAddress) i.next();
      if (target == null) {
        // Ignore nulls.
      } else if (target.equals(self)) {
        // Don't send to ourself.  Likely an error.
      } else if (t != null && !target.equals(t)) { 
        // Only resend to the specified address.
      } else {
        if (!gotContent) {
          gotContent = true;
          content = rs.getContent();
        }
	if (logger.isInfoEnabled()) {
          logger.info(
            self+": Resend"+(t==null?"*":"")+
            " to "+target+": "+rs.getUID());
        }
        sendAdd(rs, target, content);
      }
    }
  }

  private void verify(Relay.Target rt, MessageAddress s) {
    MessageAddress source = rt.getSource();
    if (source == null) return;
    if (source.equals(self)) {
      // Don't send to ourself.  Likely an error.
    } else {
      if (s == null || source.equals(s)) {
	if (logger.isInfoEnabled()) {
          logger.info(
            self+": Verify"+(s==null?"*":"")+
            " to "+source+": "+rt.getUID());
        }
        sendVerification(rt, source);
      }
    }
  }

  // ABAChange implementation
  private static final UnaryPredicate relaySourcePred =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof Relay.Source;
      }
    };

  public void abaChange(Set communities) {
    if (logger.isDebugEnabled()) logger.debug(self+": abaChange");
    Enumeration en = rootplan.searchBlackboard(relaySourcePred);
    while (en.hasMoreElements()) {
      Relay.Source rs = (Relay.Source) en.nextElement();
      Set targets = rs.getTargets();
      if (targets != null && !targets.isEmpty()) {
        Set oldTranslation = new HashSet();
        Set newTranslation = new HashSet();
        for (Iterator i = targets.iterator(); i.hasNext(); ) {
          Object o = i.next();
          if (o instanceof AttributeBasedAddress) {
            AttributeBasedAddress aba = (AttributeBasedAddress) o;
            ABATranslation abaTranslation = rootplan.getABATranslation(aba);
            if (abaTranslation != null) {
              oldTranslation.addAll(abaTranslation.getOldTranslation());
              newTranslation.addAll(abaTranslation.getCurrentTranslation());
            }
          }
        }
        if (!newTranslation.isEmpty() || !oldTranslation.isEmpty()) {
          Set adds = new HashSet(newTranslation);
          Set removes = new HashSet(oldTranslation);
          adds.removeAll(oldTranslation);
          removes.removeAll(newTranslation);
          boolean isNOP = adds.isEmpty() && removes.isEmpty();
          if (isNOP && logger.isDebugEnabled()) {
            logger.debug("old " + oldTranslation);
            logger.debug("new " + newTranslation);
            logger.debug("Rmv " + removes + " from " + rs);
            logger.debug("Add " + adds + " to " + rs);
          }
          if (!isNOP && logger.isInfoEnabled()) {
            logger.info("old " + oldTranslation);
            logger.info("new " + newTranslation);
            logger.info("Rmv " + removes + " from " + rs);
            logger.info("Add " + adds + " to " + rs);
          }
          if (!removes.isEmpty()) {
            localRemove(rs.getUID(), removes);
          }
          if (!adds.isEmpty()) {
            localAdd(rs, adds);
          }
        }
      }
    }
  }

  /** 
   * ChangeReport for this LP to identify its own changes.
   */
  private static final class MarkerReport implements ChangeReport {
    public static final MarkerReport INSTANCE = new MarkerReport();
    private MarkerReport() { }
    private Object readResolve() { return INSTANCE; }
    public String toString() { return "relay-marker-report"; }
    static final long serialVersionUID = 9091843781928322223L;
  }

  /** 
   * Token implementation, private to RelayLP.
   * <p>
   * Keeps a map of (agent-&gt;token), which allows rehydrated
   * relay objects to use "==" token matching.
   */
  private static final class TokenImpl extends Relay.Token {
    private static final Map tokens = new HashMap(13);
    private final MessageAddress addr;
    public static TokenImpl getToken(MessageAddress addr) {
      synchronized (tokens) {
        TokenImpl t = (TokenImpl) tokens.get(addr);
        if (t == null) {
          t = new TokenImpl(addr);
          tokens.put(addr, t);
        }
        return t;
      }
    }
    private TokenImpl(MessageAddress addr) { this.addr = addr; }
    private Object readResolve() { return getToken(addr); }
    public String toString() { return "<token "+addr+">"; }
    static final long serialVersionUID = 3878912876728718092L;
  }
}
