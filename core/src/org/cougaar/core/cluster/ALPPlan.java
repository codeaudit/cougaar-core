package org.cougaar.core.cluster;

import java.util.*;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Plan;

public class ALPPlan extends Subscriber
  implements
  ALPPlanServesLogicProvider,
  SubscriptionClient,
  PrivilegedClaimant
{
  protected CollectionSubscription alpPlanObjects;
  protected ClusterServesLogicProvider myCluster;
  public Distributor myDistributor;
  public static final boolean isSavePriorPublisher =
    System.getProperty("org.cougaar.core.cluster.savePriorPublisher", "false").equals("true");

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

  private final boolean consumeTuple(EnvelopeTuple tup) {
    boolean somethingFired = false;
    synchronized (subscriptions) {
      for (int i = 0, n = subscriptions.size(); i < n; i++) {
        Subscription subscription = (Subscription) subscriptions.get(i);
        somethingFired |= tup.applyToSubscription(subscription, true);
      }
    }
    callLogicProviders(tup, false);
    return somethingFired;
  }

  /** is the object non-null? **/
  private static final UnaryPredicate anythingP = new UnaryPredicate() {
    public boolean execute(Object o) {
      return (o != null);
    }
  };

  public ALPPlan(Distributor d, ClusterServesLogicProvider cluster) {
    setClientDistributor((SubscriptionClient)this, d);
    myCluster = cluster;
    myDistributor = d;
  }

  public void addXPlan(XPlanServesALPPlan xPlan) {
    if (xPlans.contains(xPlan)) return;
    xPlans.add(xPlan);
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
        throw new PublishException("ALPPlan.alpPlanObjects.add object already published: " + o.toString(),
                                   priorStack);
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
        throw new PublishException("ALPPlan.alpPlanObjects.remove object not published: " + o.toString(),
                                   priorStack);
      } else if (stacks != null) {
        stacks.put(o, new PublishStack("Prior remover: "));
      }
      return result;
    }
  }

  public final void init() {
    try {
      alpPlanObjects = new CollectionSubscription(anythingP, new AllObjectsSet(111));
      subscribe(alpPlanObjects);

      for (Iterator plans = xPlans.iterator(); plans.hasNext(); ) {
        XPlanServesALPPlan xPlan = (XPlanServesALPPlan) plans.next();
        xPlan.setupSubscriptions(this);
      }
      setReadyToPersist();
      for (Iterator eProviders = envelopeLPs.iterator(); eProviders.hasNext();){
        EnvelopeLogicProvider p = (EnvelopeLogicProvider) eProviders.next();
        try {
          p.init();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      for (Iterator mProviders = messageLPs.iterator(); mProviders.hasNext();){
        MessageLogicProvider p = (MessageLogicProvider) mProviders.next();
        try {
          p.init();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    catch (Exception ex) {
      System.err.println("Caught exception while in ALPPlan.init(): " + ex);
      ex.printStackTrace();
    }
  }

  // Subscription Client interface
  public String getSubscriptionClientName() {
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
    envelope.bulkAddObject(alpPlanObjects.getCollection());
    subscription.fill(envelope);
  }

  public void fillQuery(Subscription subscription) {
    Envelope envelope = new Envelope();
    envelope.bulkAddObject(alpPlanObjects.getCollection());
    subscription.fill(envelope);
  }

  /** Alias for sendDirective(aDirective, null);
   **/
  public void sendDirective(Directive aDirective) {
    sendQueue.add(aDirective);
  }

  /** Submit a directive with attached ChangeReports for transmission 
   * from this cluster. We fill in the ContentsId with the next available number.
   **/
  public void sendDirective(Directive aDirective, Collection c) {
    if (c != null && ((Collection) c).size()>0) {
      DirectiveMessage.DirectiveWithChangeReports dd = new DirectiveMessage.DirectiveWithChangeReports(aDirective,c);
      aDirective = dd;
    }
    sendQueue.add(aDirective);
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

  public Enumeration searchALPPlan(UnaryPredicate predicate) {
    Vector vec = new Vector();

    for (Iterator i = alpPlanObjects.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        vec.addElement(o);
      }
    }
    return vec.elements();
  }

  public int countALPPlan(UnaryPredicate predicate) {
    int c = 0;
    for (Iterator i = alpPlanObjects.getCollection().iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (predicate.execute(o)) {
        c++;
      }
    }
    return c;
  }

  /**
   * Process incoming directive messages. All messages have been
   * blessed by the message manager. The messages are implicitly
   * acknowledged by this method. The envelope of published events
   * resulting from handling the messages is returned.
   **/
  public final Envelope receiveMessages(List msgs) {
    try {
      startTransaction();
      for (Iterator iter = msgs.iterator(); iter.hasNext(); ) {
        DirectiveMessage msg = (DirectiveMessage) iter.next();
        applyMessageAgainstLogicProviders(msg);
      }

      checkUnpostedChangeReports();
      // There really should not be any change tracking subscriptions, at
      // least not in the base classes!!!  MT
      resetSubscriptionChanges(); // clear change tracking subscriptions

      return privateGetPublishedChanges();
    } finally {
      stopTransaction();
    }
  }

  private final List oneEnvelope = new ArrayList(1);

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
    System.out.println("ALPPlan restart " + cid);
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

  private void applyTuplesAgainstLogicProviders(Iterator eTuples, boolean isPersistenceEnvelope) {
    while( eTuples.hasNext() ) {
      EnvelopeTuple tuple = (EnvelopeTuple)eTuples.next();
      try {
        callLogicProviders(tuple, isPersistenceEnvelope);
      } catch (Exception e) {
        System.err.println("Caught "+e+" while running logic providers.");
        e.printStackTrace();
      }
    }
  }

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
}
