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
package org.cougaar.core.cluster;

import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.*;

// Persistence
import org.cougaar.core.cluster.persist.PersistenceNotEnabledException;
import org.cougaar.core.cluster.persist.BasePersistence;
import org.cougaar.core.cluster.persist.DatabasePersistence;
import org.cougaar.core.cluster.persist.Persistence;
import org.cougaar.core.cluster.persist.PersistenceException;

import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Plan;

import org.cougaar.domain.planning.ldm.Domain;

public class Blackboard extends Subscriber
  implements
  BlackboardServesLogicProvider,
  BlackboardClient,
  PrivilegedClaimant
{
  protected CollectionSubscription everything;
  protected ClusterServesLogicProvider myCluster;
  private Distributor myDistributor;
  public static final boolean isSavePriorPublisher =
    System.getProperty("org.cougaar.core.cluster.savePriorPublisher", "false").equals("true");
  public static final boolean enablePublishException =
    System.getProperty("org.cougaar.core.cluster.enablePublishException", "false").equals("true");

  /** The list of XPlans **/
  private Collection xPlans = new ArrayList();

  /** the queue of messages to send **/
  private List sendQueue = new ArrayList();

  // mark the envelopes which we emit so that we can detect them later.
  protected Envelope createEnvelope() {
    return new PlanEnvelope();
  }

  /** Marked Envelope class so that we can detect envelopes which we've
   * emitted.
   **/
  public static final class PlanEnvelope extends Envelope {
  }

  /** override to immediately publish deltas rather than delay until transaction close **/
  protected EnvelopeTuple clientAddedObject(Object o) {
    EnvelopeTuple tup = super.clientAddedObject(o);
    consumeTuple(tup);
    return tup;
  }

  /** override to immediately publish deltas rather than delay until transaction close **/
  protected EnvelopeTuple clientRemovedObject(Object o) {
    EnvelopeTuple tup = super.clientRemovedObject(o);
    consumeTuple(tup);
    return tup;
  }

  /** override to immediately publish deltas rather than delay until transaction close **/
  protected EnvelopeTuple clientChangedObject(Object o, List changes) {
    EnvelopeTuple tup = super.clientChangedObject(o, changes);
    consumeTuple(tup);
    return tup;
  }

  /** invoked via client*Object while executing an LP **/
  private final boolean consumeTuple(EnvelopeTuple tup) {
    boolean somethingFired = false;
    synchronized (subscriptions) {
      for (int i = 0, n = subscriptions.size(); i < n; i++) {
        Subscription subscription = (Subscription) subscriptions.get(i);
        somethingFired |= tup.applyToSubscription(subscription, true);
      }
    }
    // recurses
    callLogicProviders(tup, false);
    return somethingFired;
  }

  /** is the object non-null? **/
  private static final UnaryPredicate anythingP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o != null);
    }
  };

  public Blackboard(ClusterServesLogicProvider cluster, Object state) {
    myDistributor = createDistributor(cluster, state);
    setClientDistributor((BlackboardClient)this, myDistributor);
    myCluster = cluster;
  }

  public void addXPlan(XPlanServesBlackboard xPlan) {
    if (xPlans.contains(xPlan)) return;
    xPlans.add(xPlan);
    if (xPlan instanceof SupportsDelayedLPActions) {
      dlaPlans.add(xPlan);
    }
    xPlan.setupSubscriptions(this);
    setReadyToPersist();
  }

  public Collection getXPlans() {
    return xPlans;
  }

  private static class AllObjectsSet extends HashSet {
    Map stacks = createStackMap();
    protected Map createStackMap() {
      if (isSavePriorPublisher) {
        return new HashMap();
      } else {
        return null;              // Don't keep prior publishing info
      }
    }

    public AllObjectsSet(int size) {
      super(size);
    }
    public boolean add(Object o) {
      boolean result = super.add(o);
      if (!result) {
        PublishStack priorStack = null;
        if (stacks != null) {
          priorStack = (PublishStack) stacks.get(o);
        }
        throw new PublishException("Blackboard.everything.add object already published: " + o.toString(),
                                   priorStack, stacks != null);
      } else if (stacks != null) {
        stacks.put(o, new PublishStack("Prior publisher: "));
      }
      return result;
    }
    public boolean remove(Object o) {
      boolean result = super.remove(o);
      if (!result) {
        PublishStack priorStack = null;
        if (stacks != null) {
          priorStack = (PublishStack) stacks.get(o);
        }
        throw new PublishException("Blackboard.everything.remove object not published: " + o.toString(),
                                   priorStack, stacks != null);
      } else if (stacks != null) {
        stacks.put(o, new PublishStack("Prior remover: "));
      }
      return result;
    }
  }

  public final void init() {
    everything =
      new CollectionSubscription(anythingP,
                                 enablePublishException
                                 ? new AllObjectsSet(111)
                                 : new HashSet(111));
    subscribe(everything);
    setReadyToPersist();
  }

  // Subscription Client interface
  public String getBlackboardClientName() {
    return getClass().getName();
  }

  /**
   * Provide a new subscription with its initial fill. Called under
   * the protection of the distributor lock so we are guaranteed that
   * the allPlanObjects won't change.
   **/
  public void fillSubscription(Subscription subscription) {
    if (subscription.getSubscriber() == this) return; // Don't fill ourselves
    Envelope envelope = new Envelope();
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  public void fillQuery(Subscription subscription) {
    Envelope envelope = new Envelope();
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  /** Alias for sendDirective(aDirective, null);
   **/
  public void sendDirective(Directive aDirective) {
    if (aDirective == null) {
      throw new IllegalArgumentException("directive must not be null.");
    } else {
      sendQueue.add(aDirective);
    }
  }

  /** Submit a directive with attached ChangeReports for transmission 
   * from this cluster. We fill in the ContentsId with the next available number.
   **/
  public void sendDirective(Directive aDirective, Collection c) {
    if (aDirective == null) {
      throw new IllegalArgumentException("directive must not be null.");
    } else {
      if (c != null && ((Collection) c).size()>0) {
        DirectiveMessage.DirectiveWithChangeReports dd = new DirectiveMessage.DirectiveWithChangeReports(aDirective,c);
        aDirective = dd;
      }
      sendQueue.add(aDirective);
    }
  }

  public long currentTimeMillis() {
    return myCluster.currentTimeMillis();
  }

  /**
   * Add Object to the LogPlan Collection
   **/
  public void add(Object o) {
    publishAdd(o);
  }

  /** Removed Object to the LogPlan Collection
   **/
  public void remove(Object o) {
    publishRemove(o);
  }

  /** Change Object to the LogPlan Collection
   **/
  public void change(Object o) {
    publishChange(o,null);
  }
  public void change(Object o, Collection changes) {
    publishChange(o, changes);
  }

  public Enumeration searchBlackboard(UnaryPredicate predicate) {
    Vector vec = new Vector();

    for (Iterator i = everything.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        vec.addElement(o);
      }
    }
    return vec.elements();
  }

  public int countBlackboard(UnaryPredicate predicate) {
    int c = 0;
    for (Iterator i = everything.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        c++;
      }
    }
    return c;
  }

  public int getBlackboardSize() {
    return everything.size();
  }

  /**
   * Process incoming directive messages. All messages have been
   * blessed by the message manager. The messages are implicitly
   * acknowledged by this method. The envelope of published events
   * resulting from handling the messages is returned.
   **/
  public final Envelope receiveMessages(List msgs) {
    //try {
    //  startTransaction();
    for (Iterator iter = msgs.iterator(); iter.hasNext(); ) {
      DirectiveMessage msg = (DirectiveMessage) iter.next();
      applyMessageAgainstLogicProviders(msg);
    }

    checkUnpostedChangeReports();
    // There really should not be any change tracking subscriptions, at
    // least not in the base classes!!!  MT
    resetSubscriptionChanges(); // clear change tracking subscriptions
    
    return privateGetPublishedChanges();
    //} finally {
    //  stopTransaction();
    //}
  }

  private final List oneEnvelope = new ArrayList(1);

  /** called by transaction close within the thread of PlugIns.  
   * Also called at the end of an LP pseudo-transaction, but
   * most of the logic here is disabled in that case.
   **/
  public final Envelope receiveEnvelope(Envelope envelope) {
    oneEnvelope.add(envelope);
    super.receiveEnvelopes(oneEnvelope); // Move to our inbox
    oneEnvelope.clear();

    if (! (envelope instanceof PlanEnvelope)) {
      // although we aways consume envelopes, we only act on them
      // when we didn't generate 'em
      privateUpdateSubscriptions();

      try {
        boolean isPersistenceEnvelope = envelope instanceof PersistenceEnvelope;
        List tuples = envelope.getRawDeltas();
        int l = tuples.size();
        for (int i = 0; i<l; i++) {
          try {
            callLogicProviders((EnvelopeTuple) tuples.get(i), isPersistenceEnvelope);
          } catch (Exception e) {
            System.err.println("Caught " + e + " while running logic providers.");
            e.printStackTrace();
          }
        }
      } finally {
        // clear subscriptions deltas, just in case.
        resetSubscriptionChanges();
      }
    }

    return privateGetPublishedChanges();
  }

  private final HashMap directivesByDestination = new HashMap(89);

  public void appendMessagesToSend(List messages) {
    for (Iterator iter = sendQueue.iterator(); iter.hasNext(); ) {
      Directive dir = (Directive) iter.next();
      ClusterIdentifier dest = dir.getDestination();
      ArrayList dirs = (ArrayList) directivesByDestination.get(dest);
      if (dirs == null) {
        dirs = new ArrayList();
        directivesByDestination.put(dest, dirs);
      }
      dirs.add(dir);
    }
    for (Iterator iter = directivesByDestination.keySet().iterator(); iter.hasNext(); ) {
      ClusterIdentifier dest = (ClusterIdentifier) iter.next();
      ArrayList dirs = (ArrayList) directivesByDestination.get(dest);
      int size = dirs.size();
      if (size > 0) {
        Directive[] directives = (Directive[]) dirs.toArray(new Directive[size]);
        DirectiveMessage ndm = new DirectiveMessage(directives);
        ndm.setDestination(dest);
        ndm.setSource(myCluster.getClusterIdentifier());
        messages.add(ndm);
        dirs.clear();
      }
    }
    sendQueue.clear();
  }

  public void restart(ClusterIdentifier cid) {
    System.out.println("Blackboard restart " + cid);
    for (int i = 0, n = restartLPs.size(); i < n; i++) {
      RestartLogicProvider p = (RestartLogicProvider) restartLPs.get(i);
      try {
        p.restart(cid);
      }
      catch (RuntimeException e) {
        e.printStackTrace();
      }
    }
  }

  private final List envelopeLPs = new ArrayList();
  private final List messageLPs = new ArrayList();
  private final List restartLPs = new ArrayList();

  // default protection
  void addLogicProvider(LogicProvider lp) {
    if (lp instanceof MessageLogicProvider) {
      messageLPs.add(lp);
    }
    if (lp instanceof EnvelopeLogicProvider) {
      envelopeLPs.add(lp);
    }
    if (lp instanceof RestartLogicProvider) {
      restartLPs.add(lp);
    }
    lp.init();
  }

  private void applyMessageAgainstLogicProviders(DirectiveMessage m) {
    Directive[] directives = m.getDirectives();
    int l = messageLPs.size();
    for (int i = 0; i < l; i++) {
      MessageLogicProvider p = (MessageLogicProvider) messageLPs.get(i);
      try {
        for (int j = 0; j < directives.length; j++) {
          Directive d = directives[j];
          Collection cc = null;
          if (d instanceof DirectiveMessage.DirectiveWithChangeReports) {
            DirectiveMessage.DirectiveWithChangeReports dd = (DirectiveMessage.DirectiveWithChangeReports) d;
            cc = dd.getChangeReports();
            d = dd.getDirective();
          }
          p.execute(d,cc);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /** called by receiveEnvelope (on behalf of a plugin) and consumeTuple (on behalf of
   * an LP).
   **/
  private void callLogicProviders(EnvelopeTuple obj, boolean isPersistenceEnvelope) {
    Collection changes = null;
    if (obj instanceof ChangeEnvelopeTuple) {
      changes = ((ChangeEnvelopeTuple)obj).getChangeReports();
    }
    synchronized( envelopeLPs ) {
      int l = envelopeLPs.size();
      for (int i=0; i<l; i++) {
        EnvelopeLogicProvider p = (EnvelopeLogicProvider) envelopeLPs.get(i);
	if (isPersistenceEnvelope && !(p instanceof LogicProviderNeedingPersistenceEnvelopes)) {
	  continue;	// This lp does not want contents of PersistenceEnvelopes
	}
        try {
          p.execute(obj, changes);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  // handle events - right now do nothing.
  public boolean triggerEvent(Object event) {
    return false;
  }

  public PublishHistory getHistory() {
    return myDistributor.history;
  }

  // support delayed LP actions
  private ArrayList dlaPlans = new ArrayList(1);

  Envelope executeDelayedLPActions() {
    int l = dlaPlans.size();
    for (int i=0; i<l; i++) {
      SupportsDelayedLPActions p = (SupportsDelayedLPActions) dlaPlans.get(i);
      p.executeDelayedLPActions();
    }

    return privateGetPublishedChanges();
  }

  public Object getState() throws PersistenceNotEnabledException {
    return myDistributor.getState();
  }

  //
  // domain client support
  //
  private final HashMap domains = new HashMap(11);
  private static class DomainTuple {
    public Domain domain;
    public XPlanServesBlackboard xplan;
    public Collection lps;
    public DomainTuple(Domain domain, XPlanServesBlackboard xplan, Collection lps) {
      this.domain = domain;
      this.xplan = xplan;
      this.lps = lps;
    }
  }

  public XPlanServesBlackboard getXPlanForDomain(Domain d) {
    synchronized (domains) {
      DomainTuple dt = (DomainTuple) domains.get(d);
      if (dt != null) {
        return dt.xplan;
      } else {
        return null;
      }
    }
  }

  /** Adds the domain to the blackboard by adding any XPlan required and
   * any LogicProviders.
   **/
  public void connectDomain(Domain d) {
    synchronized (domains) {
      if (domains.get(d) != null) {
        throw new RuntimeException("Tried to re-add domain "+d);
      } 
      
      DomainTuple dt = null;
      // create the XPlan
      XPlanServesBlackboard xplan = d.createXPlan(getXPlans());
      if (xPlans.contains(xplan)) {
        // in fact, domains often just use some other domain's xplan.
        // we should probably forbid this at some point.

        //throw new RuntimeException("Domain "+d+" tried to reuse XPlan "+xplan);
        //System.err.println("Warning: Domain "+d+" tried to reuse XPlan "+xplan);
      }
        
      // create the LPs and hook them in
      Collection lps = new ArrayList(d.createLogicProviders(xplan, myCluster));

      // add the domainTuple
      dt = new DomainTuple(d, xplan, lps);
      domains.put(d,dt);

      // activate the xplan
      if (xplan != null) addXPlan(xplan);

      // activate the LPs
      if (lps != null) {
        for (Iterator li = lps.iterator(); li.hasNext(); ) {
          Object lo = li.next();
          if (lo instanceof LogicProvider) {
            addLogicProvider((LogicProvider) lo);
          } else {
            System.err.println("Domain "+d+" requested loading of a non LogicProvider "+lo+" (Ignored).");
          }
        }
      }
    }
  }
  

  //
  // Distributor
  //
  private Distributor createDistributor(
      ClusterServesLogicProvider cluster,
      Object state) {
    Distributor d = new Distributor(this, cluster.getClusterIdentifier().getAddress());
    Persistence persistence = createPersistence(cluster);
    boolean lazyPersistence = System.getProperty("org.cougaar.core.cluster.persistence.lazy", "true").equals("true");
    d.setPersistence(persistence, lazyPersistence);
    d.start(cluster, state);       // cluster, state

    return d;
  }

  public Distributor getDistributor() {
    return myDistributor;
  }

  protected Persistence createPersistence(ClusterServesLogicProvider cluster) {
    if (System.getProperty("org.cougaar.core.cluster.persistence.enable", "false").equals("false"))
      return null;		// Disable persistence for now
    try {
      Persistence result = BasePersistence.find(cluster);
      if (System.getProperty("org.cougaar.core.cluster.persistence.disableWrite", "false").equals("true")) {
        result.disableWrite();
      }
      return result;
    }
    catch (PersistenceException e) {
      e.printStackTrace();
    }
    return null;
  }

}
