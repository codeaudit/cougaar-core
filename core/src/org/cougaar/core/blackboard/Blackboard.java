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
package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import java.util.*;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.component.ServiceBroker;
// Persistence
import org.cougaar.core.persist.PersistenceNotEnabledException;
import org.cougaar.core.persist.BasePersistence;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceException;
import org.cougaar.util.log.*;
import org.cougaar.core.logging.LoggingServiceWithPrefix;

import org.cougaar.core.service.community.CommunityService;
import org.cougaar.core.service.DomainForBlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.Plan;

import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.core.mts.MessageAttributes;

import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.SearchResult;
import javax.naming.directory.BasicAttribute;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.DirContext;
import javax.naming.event.EventContext;
// import javax.naming.event.NamespaceChangeListener;
// import javax.naming.event.NamingEvent;
// import javax.naming.event.NamingExceptionEvent;
// import javax.naming.event.NamingListener;
// import javax.naming.event.ObjectChangeListener;
import org.cougaar.core.service.community.*;

/** The Blackboard
 *
 * @property org.cougaar.core.agent.savePriorPublisher When set to <em>true</em>, will collect extra 
 * information on each publish to detect problems with multiple adds, deletes, etc by complaining
 * about unexpected state changes.  This adds significant runtime overhead.
 * @property org.cougaar.core.agent.enablePublishException When set to <em>true</em>, collects stack frames
 * for each published object in order to pinpoint both sides of publish conflicts.  This is <em>extremely</em> 
 * expensive.
 * @property org.cougaar.core.persistence.enable When set to <em>true</em> will enable blackboard persistence.
 * @property org.cougaar.core.persistence.disableWrite See documentation on Blackboard persistence.
 * @property org.cougaar.core.persistence.sequence See documentation on Blackboard persistence.
 **/
public class Blackboard extends Subscriber
  implements
  BlackboardServesLogicProvider,
  BlackboardClient,
  PrivilegedClaimant
{
  protected CollectionSubscription everything;
  protected ClusterServesLogicProvider myCluster;
  private Distributor myDistributor;
  protected ServiceBroker myServiceBroker;
  protected DomainForBlackboardService myDomainService;
  protected LoggingService logger;

  public static final boolean isSavePriorPublisher =
    System.getProperty("org.cougaar.core.agent.savePriorPublisher", "false").equals("true");
  public static final boolean enablePublishException =
    System.getProperty("org.cougaar.core.agent.enablePublishException", "false").equals("true");


  /** the queue of messages to send **/
  private List sendQueue = new ArrayList();

  // mark the envelopes which we emit so that we can detect them later.
  protected Envelope createEnvelope() {
    if (isTimestamped()) {
      return new TimestampedPlanEnvelopeImpl();
    } else {
      return new PlanEnvelopeImpl();
    }
  }

  /** Marked Envelope <i>interface</i> so that we can detect envelopes which we've
   * emitted.
   *
   * This isn't an Envelope, since Envelope is a class, but this interface
   * will only be applied to the two Envelope subclasses listed below.
   **/
  interface PlanEnvelope {
  }

  private static final class PlanEnvelopeImpl 
    extends Envelope implements PlanEnvelope {
    }

  private static final class TimestampedPlanEnvelopeImpl
    extends TimestampedEnvelope implements PlanEnvelope {
      public boolean isBlackboard() { return true; }
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

  public Blackboard(ClusterServesLogicProvider cluster, ServiceBroker sb, Object state) {
    myServiceBroker = sb;
    myDistributor = createDistributor(cluster, state);
    setClientDistributor((BlackboardClient) this, myDistributor);
    setName("<blackboard>");
    myCluster = cluster;
    logger = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(logger, myCluster.getClusterIdentifier().toString() + ": ");
    myDomainService = 
      (DomainForBlackboardService) sb.getService(this, 
                                                 DomainForBlackboardService.class, 
                                                 null);
    if (myDomainService == null) {
      RuntimeException re = 
        new RuntimeException("Couldn't get DomainForBlackboardService!");
      re.printStackTrace();
      throw re;
    }
  }

  public void stop() {
    stopDistributor(myDistributor);
    myDistributor = null;
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
    if (subscription == everything) {
      return; // Don't fill ourselves
    }
    Envelope envelope = createQueryEnvelope(subscription);
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  public void fillQuery(Subscription subscription) {
    Envelope envelope = createQueryEnvelope(subscription);
    envelope.bulkAddObject(everything.getCollection());
    subscription.fill(envelope);
  }

  private Envelope createQueryEnvelope(Subscription subscription) {
    if (isTimestamped()) {
      TimestampedEnvelope te = new TimestampedEnvelope();
      Subscriber subscriber = subscription.getSubscriber();
      if (subscriber != null) {
        te.setName(subscriber.getName());
      }
      long nowTime = System.currentTimeMillis();
      te.setTransactionOpenTime(nowTime);
      // should we wait until after the query to set the close time?
      te.setTransactionCloseTime(nowTime);
      return te;
    } else {
      return new Envelope();
    }
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

  /**
   * called by distributor to prepare for "receiveEnvelope(..)" calls.
   */
  public final void prepareForEnvelopes() {
    setTransactionOpenTime();
  }

  /** called by transaction close within the thread of Plugins.  
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
  
  /*
   * Builds up hashmap of arrays of directives for each agent, <code>ClusterIdentifier</code>.
   * Modified to handle destinations of <code>AttributeBasedAddress</code>es, so that these are 
   * sent properly as well. 
   */
  public void appendMessagesToSend(List messages) {
    
    // FIXME - prefill cache of aba roles to ClusterIDs here, instead of building up a cache
    // fillCache();
    
    for (Iterator iter = sendQueue.iterator(); iter.hasNext(); ) {
      Directive dir = (Directive) iter.next();  
      ClusterIdentifier dest = dir.getDestination();
      
      // get all destinations
      
      /**
       * If dest is an ABA, get all agent_names from cache or 
       * nameserver and fills in the hashmap of directives
       * Short and easy way to handle ABA destinations
       */
      ArrayList dirs;

      if(dest instanceof AttributeBasedAddress) {
        //System.out.println("-------BLACKBOARD ENCOUNTERED ABA-----");
        MessageAttributes qosAttributes = dest.getQosAttributes();
	Collection agents = getABAAddresses((AttributeBasedAddress) dest);   // List of CIs
	// for all destinations, add a new directive array and insert a new directive, or add to 
	// an existing array in the destinations hashmap
	for (Iterator i = agents.iterator(); i.hasNext(); ) {
          ClusterIdentifier agentAddress = (ClusterIdentifier) i.next();
          if (qosAttributes != null) {
            agentAddress = new ClusterIdentifier(qosAttributes, agentAddress.getAddress());
          }
	  dirs = (ArrayList)directivesByDestination.get(agentAddress);
	  if (dirs == null) {
	    dirs = new ArrayList();
	    directivesByDestination.put(agentAddress, dirs); 
	  }
	  dirs.add(dir);
	}
      } // done with aba handling
      
      /**
       * dest is regular ClusterID so proceed as before 
       */
      else {
	dirs = (ArrayList) directivesByDestination.get(dest);
	if (dirs == null) {
	  dirs = new ArrayList();
	  directivesByDestination.put(dest, dirs);
	}
	dirs.add(dir);	
      }
    }
    /**
     * By now directivesByDestination only has ArrayLists of ClusterIdentifiers,
     * so we can set their directives as before. 
     */
    for (Iterator iter = directivesByDestination.keySet().iterator(); iter.hasNext(); ) {
      ClusterIdentifier tmpci = (ClusterIdentifier) iter.next();
      ArrayList dirs = (ArrayList) directivesByDestination.get(tmpci);
      int size = dirs.size();
      if (size > 0) {
        Directive[] directives = (Directive[]) dirs.toArray(new Directive[size]);
        DirectiveMessage ndm = new DirectiveMessage(directives);
	ndm.setDestination(tmpci);
        ndm.setSource(myCluster.getClusterIdentifier());
        messages.add(ndm);
        dirs.clear();
      }
    }
    sendQueue.clear();
  }

  public void restart(ClusterIdentifier cid) {
    myDomainService.invokeRestartLogicProviders(cid);
  }

  private void applyMessageAgainstLogicProviders(DirectiveMessage m) {
    myDomainService.invokeMessageLogicProviders(m);
  }

  /** called by receiveEnvelope (on behalf of a plugin) and consumeTuple (on behalf of
   * an LP).
   **/
  private void callLogicProviders(EnvelopeTuple obj, boolean isPersistenceEnvelope) {
    myDomainService.invokeEnvelopeLogicProviders(obj, isPersistenceEnvelope);
  }

  // handle events - right now do nothing.
  public boolean triggerEvent(Object event) {
    return false;
  }

  public PublishHistory getHistory() {
    return myDistributor.history;
  }

  protected Envelope executeDelayedLPActions() {
    myDomainService.invokeDelayedLPActions();
    return privateGetPublishedChanges();
  }

  public Object getState() throws PersistenceNotEnabledException {
    return myDistributor.getState();
  }

  /** Ensure that all the domains know that this is THE blackboard
   **/
  protected void connectDomains() {
    myDomainService.setBlackboard(this);
    setReadyToPersist();
  }
  

  //
  // Distributor
  //
  private Distributor createDistributor(
      ClusterServesLogicProvider cluster,
      Object state) {
    Distributor d = new Distributor(this, cluster.getClusterIdentifier().getAddress());
    Persistence persistence = createPersistence(cluster);
    boolean lazyPersistence = System.getProperty("org.cougaar.core.persistence.lazy", "true").equals("true");
    d.setPersistence(persistence, lazyPersistence);
    d.start(cluster, state);       // cluster, state

    return d;
  }

  private void stopDistributor(Distributor d) {
    if (d != null) {
      d.stop();
    }
  }

  public Distributor getDistributor() {
    return myDistributor;
  }

  protected Persistence createPersistence(ClusterServesLogicProvider cluster) {
    try {
      Persistence result = BasePersistence.find(cluster, myServiceBroker);
      if (System.getProperty("org.cougaar.core.persistence.disableWrite", "false").equals("true")) {
        String sequence =
          System.getProperty("org.cougaar.core.persistence.sequence", "");
        result.disableWrite(sequence);
      }
      return result;
    }
    catch (PersistenceException e) {
      e.printStackTrace();
    }
    return null;
  }
 
  
  // -------- Methods for ABA Handling Below --------  needs work //
  
  // AttributeBasedAddress to ABATranslation cache
  private Map cache = new HashMap(89);

  private CommunityService communityService;
  private boolean haveCommunityService = false;
//   private NamingService namingService;
  private CommunityChangeListener communityChangeListener;
  private EventContext communityContext;
  private CacheClearer cacheClearer = new CacheClearer();
  private Object cacheClearerLock = new Object();

  private static class ABATranslationImpl implements ABATranslation {
    Collection old, current;
    ABATranslationImpl(Collection current) {
      this.current = current;
    }
    public Collection getOldTranslation() {
      return old;
    }
    public Collection getCurrentTranslation() {
      return current;
    }
    void setCurrentTranslation(Collection newCurrentTranslation) {
      current = newCurrentTranslation;
    }
    void setOldTranslation(Collection newOldTranslation) {
      old = newOldTranslation;
    }
    boolean isEmpty() {
      return old == null && current == null;
    }
  }

//   private class MyNamingListener implements NamespaceChangeListener, ObjectChangeListener {
//     public void objectAdded(NamingEvent evt) {
//       try {
//         String name = evt.getNewBinding().getName();
//         getCommunityContext().addNamingListener(name, EventContext.OBJECT_SCOPE, namingListener);
//         if (logger.isDebugEnabled()) logger.debug("Blackboard installing NamingListener to added " + name);
//       } catch (NamingException ne) {
//         logger.error("Error installing naming listener", ne);
//       }
//       clearCache();
//     }
//     public void objectRemoved(NamingEvent evt) {
//       clearCache();
//     }
//     public void objectRenamed(NamingEvent evt) {
//       clearCache();
//     }
//     public void objectChanged(NamingEvent evt) {
//       clearCache();
//     }
//     public void namingExceptionThrown(NamingExceptionEvent evt) {
//     }
//   }
  
  private class MyCommunityChangeListener extends CommunityChangeAdapter {
    public void communityChanged(CommunityChangeEvent e) {
      if (logger.isDebugEnabled()) logger.debug(e.toString());
      clearCache(e.getCommunityName());
    }
  }

  /*
   * Loops through the cache of ABAs and returns ClusterIdentifiers, 
   * else it querries the nameserver for all agents with the ABA's role attribute, 
   * and builds the cache.
   * @return list (copy) of the addresses of the agents matching the ABA
   */
  public Collection getABAAddresses(AttributeBasedAddress aba) {
    // first look in cache
    Collection matches = null;
    synchronized (cache) {
      ABATranslation abaTranslation = (ABATranslation) cache.get(aba);
      if (abaTranslation != null){
        matches = abaTranslation.getCurrentTranslation();
      }
    }
    if (matches == null) {
      // Not in cache. Get it the hard way from community service
      matches = lookupABA(aba);
      if (logger.isDebugEnabled()) {
        logger.debug("lookupABA: " + aba + "->" + matches);
      }
      synchronized (cache) {
        ABATranslationImpl abaTranslation = (ABATranslationImpl) cache.get(aba);
        if (abaTranslation == null) {
          abaTranslation = new ABATranslationImpl(matches);
          cache.put(aba, abaTranslation);
        } else {
          abaTranslation.setCurrentTranslation(matches);
        }
      }
    }
    return new ArrayList(matches); // Return a copy to preserve cache integrity
  }

//   private EventContext getCommunityContext() throws NamingException {
//     synchronized (cache) {
//       if (communityContext == null) {
//         try {
//           DirContext rootContext = (DirContext) namingService.getRootContext();
//           communityContext = (EventContext)
//             rootContext.lookup(CommunityService.COMMUNITIES_CONTEXT_NAME);
//         } catch (NamingException ne) {
//           throw ne;
//         } catch (Exception e) {
//           NamingException x = 
//             new NamingException("Unable to access name-server");
//           x.setRootCause(e);
//           throw x;
//         }
//       }
//       return communityContext;
//     }
//   } 
    
  // get the CommunityService when possible 
  private CommunityService _myCommunityService = null;

  private CommunityService getCommunityService() {
    if (_myCommunityService != null) {
      return _myCommunityService;
    } else {
      _myCommunityService = (CommunityService)
        myServiceBroker.getService(this, CommunityService.class, null);
      if (_myCommunityService == null) {
        getLogger().warn("Warning: Blackboard had no CommunityService - will fall back to dynamic service lookup.  Risk of Deadlock!", new Throwable());
      }
      _myCommunityService.addListener(new MyCommunityChangeListener());
      return _myCommunityService;
    }
  }

  private Logger getLogger() {
    return Logging.getLogger(Blackboard.class);
  }

  /*
   * Queries NameServer and gets a collection ClusterIdentifiers of
   * all agents having the attribute type and value specified by the
   * ABA and stores the collection in the ABA cache. Returns a List
   * (copy) of the addresses found.
   * @return list (copy) of the addresses of the agents matching the ABA
   */
  private Collection lookupABA(AttributeBasedAddress aba) {
    CommunityService cs = getCommunityService();
    String communitySpec = getCommunitySpec(aba);
    String roleValue = aba.getAttributeValue();
    String roleName = aba.getAttributeType();
    String filter = "(" + roleName + "=" + roleValue + ")";
    
    if (cs == null) {
      return Collections.EMPTY_SET;
    }
    Collection matches = cs.search(communitySpec, filter);
    List cis = new ArrayList(matches.size());
    for (Iterator i = matches.iterator(); i.hasNext(); ) {
      ClusterIdentifier cid = (ClusterIdentifier) i.next();
      cis.add(cid);
    }
    return cis;
  }

  private void clearCache(String communityName) {
    synchronized (cacheClearerLock) {
      if (cacheClearer == null) {
        cacheClearer = new CacheClearer();
      }
    }
    cacheClearer.add(communityName);
  }

  private class CacheClearer implements Runnable {
    private Set changedCommunities = new HashSet();
    private Thread thread;
    
    public synchronized void add(String communityName) {
      changedCommunities.add(communityName);
      if (thread == null) {
        thread = new Thread(this, "ABA Cache Clearer");
        thread.start();
      } else {
        notify();
      }
    }

    public void run() {
      Set changes = new HashSet();
      while (true) {
        synchronized (this) {
          if (changedCommunities.size() == 0) {
            try {
              wait(10000L);
            } catch (InterruptedException ie) {
            }
          }
          if (changedCommunities.size() == 0) {
            thread = null;
            return;
          }
          try {
            Thread.sleep(1000L);
          } catch (InterruptedException ie) {
          }
          changes.addAll(changedCommunities);
          changedCommunities.clear();
        }
        myDistributor.invokeABAChangeLPs(changes);
        changes.clear();
      }
    }
  }

  public void invokeABAChangeLPs(Set communities) {
    synchronized (cache) {
      for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
        ABATranslationImpl at = (ABATranslationImpl) i.next();
        at.setOldTranslation(at.getCurrentTranslation());
        at.setCurrentTranslation(null); // Filled in when needed
      }
      myDomainService.invokeABAChangeLogicProviders(communities);
      for (Iterator i = cache.values().iterator(); i.hasNext(); ) {
        ABATranslationImpl at = (ABATranslationImpl) i.next();
        at.setOldTranslation(null);
        if (at.isEmpty()) i.remove();
      }
    }
  }

  public ABATranslation getABATranslation(AttributeBasedAddress aba) {
    synchronized (cache) {
      ABATranslationImpl ret = (ABATranslationImpl) cache.get(aba);
      if (ret.getOldTranslation() == null) return null;
      if (ret.getCurrentTranslation() == null) {
        ret.setCurrentTranslation(lookupABA(aba));
      }
      return ret;
    }
  }

  // Stub - should be replaced when we figure out semantics for
  // community name spec in the aba.d
  protected String getCommunitySpec(AttributeBasedAddress aba) {
    String abaComm = aba.getCommunityName();

    if ((abaComm == null) ||
        (abaComm.equals("")) ||
        (abaComm.equals("*"))) {
      return "";
    } else {
      return abaComm;
    }
  }
}
