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

import org.cougaar.core.service.DomainForBlackboardService;
import org.cougaar.core.service.NamingService;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.Plan;

import org.cougaar.multicast.AttributeBasedAddress;

import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.SearchResult;
import javax.naming.directory.BasicAttribute;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.DirContext;

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

  public static final boolean isSavePriorPublisher =
    System.getProperty("org.cougaar.core.agent.savePriorPublisher", "false").equals("true");
  public static final boolean enablePublishException =
    System.getProperty("org.cougaar.core.agent.enablePublishException", "false").equals("true");


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

  public Blackboard(ClusterServesLogicProvider cluster, ServiceBroker sb, Object state) {
    myServiceBroker = sb;
    myDistributor = createDistributor(cluster, state);
    setClientDistributor((BlackboardClient) this, myDistributor);
    myCluster = cluster;
    
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
	List agents = getAllAddresses((AttributeBasedAddress)dest);   // List of CIs
	// for all destinations, add a new directive array and insert a new directive, or add to 
	// an existing array in the destinations hashmap
	for (int i=0; i < agents.size(); i++) {
	  dirs = (ArrayList)directivesByDestination.get(agents.get(i));
	  if(dirs == null){
	    dirs = new ArrayList();
	    directivesByDestination.put(agents.get(i), dirs); 
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
    if (System.getProperty("org.cougaar.core.persistence.enable", "false").equals("false"))
      return null;		// Disable persistence for now
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
  
  // (String)role to (List)agentnames cache
  private static java.util.HashMap cache = new java.util.HashMap(89);
  
  /*
   * Loops through the cache of ABAs and returns ClusterIdentifiers, 
   * else it querries the nameserver for all agents with the ABA's role attribute, 
   * and builds the cache. 
   */
  public List getAllAddresses(AttributeBasedAddress dest) {
    
    ArrayList l = null; // clusterids to be returned
    
    // first look in cache

    // return a List of agent_names for the matching roleValue in HashMap 
    synchronized (cache) {
      for (Iterator i = cache.entrySet().iterator(); i.hasNext(); ) {
        Map.Entry ent = (Map.Entry) i.next();
	String roleValue = (String)ent.getKey();
	//System.out.println("roleValue is: " + roleValue);
	
	// if ABA's role = rolevalue, get out of hashmap a set of clusterids
	if(dest.getAttributeValue().equals(roleValue)) {
	  List values = (ArrayList) ent.getValue();
	  if(values != null) {
	    l = new ArrayList();
	    for (Iterator iter = values.iterator(); iter.hasNext(); ) {
	      l.add(iter.next());
	    }
	  }
	}
      }
    }
    
    
    // else query NameServer
    if(l != null){
      //System.out.println("Returning from cache");
      return l;
    }
    else {
      l = lookupABAinNameServer(dest);
      //System.out.println("Returning from NameServer");
      return l;
    }
  }
  
  /*
   * Associated a list of ClusterIDs with a role in the aba cache. 
   */
  public static void cacheByRole(String roleValue, List agent_names) {
    synchronized (cache) {
      cache.put(roleValue, agent_names);
    }
  } 
    
  /*
   * Querries NameServer and returns a list of all agentnames matching the aba's
   * String role value. Adds new list to the aba cache. 
   */
  public ArrayList lookupABAinNameServer(AttributeBasedAddress aba) {
    
    ArrayList cis = new ArrayList();
    String roleValue = aba.getAttributeValue();
    String roleName = aba.getAttributeName();
    //System.out.println("Looking up ABA " + roleName + " = " + roleValue + " in NameServer.");
    
    DirContext dirContext = getNameServer();
    if (dirContext==null)
      System.err.println("dirContext in lookupABAinNameServer is null!!");
    
    try {
      SearchControls boundValueSearchControls = new SearchControls();
      boundValueSearchControls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
      boundValueSearchControls.setReturningObjFlag(true);
      NamingEnumeration enum = dirContext.search("", 
                                                 "(" + roleName + 
                                                 "=" + roleValue + ")",
                                                 boundValueSearchControls);
      while (enum.hasMore()) {
        SearchResult result = (SearchResult) enum.next();
	cis.add(result.getObject());
        /*
        System.out.println("Name: " + result.getName() + 
                           " object: " + result.getObject() + 
                           " objectClass: " + result.getClassName()); */
			   } 
    } catch (Exception e) {
      e.printStackTrace();
    }
    cacheByRole(roleValue, cis);
    
    return cis;
  }
  
  
  /*
   * Return a reference to the NameServer. 
   */
  public DirContext getNameServer() {
    
    NamingService myNamingService;
    String MNR_CONTEXT_NAME = "MNRTest";
    String ROLE_ATTRIBUTE_NAME = "Role";
    
    myNamingService = (NamingService)myServiceBroker.getService(this, NamingService.class, null);

    DirContext dirContext = null;
   
    try {
      dirContext = 
        (DirContext) myNamingService.getRootContext().lookup(MNR_CONTEXT_NAME);
    } catch (NamingException ne) {
      // Ignore for now - if it hasn't been created, we obviously can't send the abas. 
    }
    
    return dirContext;
  }

 /*
   * FIXME - this should be called fist to fill cache initially instead 
   * of building it, as it is now implemented. 
   *
   * Fills up cache of roleValue (ie 'Manager') to a set of ClusterIdentifiers 
   * by queries to the NameServer. 
   
  public void fillCache() {
    // for all roles
    // set agents = search by role type
    // cacheByRole(role, agents);
    
    DirContext dirContext = getNameServer();
    
    // get all object names, then search attributes on them
    try {
      //NamingEnumeration enum = dirContext.list("MNRTest");
      //while (enum.hasMore()) {
      //SearchResult result = (SearchResult) enum.next();
      //System.out.println("Object Name = " + result.getName());
      //}
      if(dirContext==null)
	System.out.println("dirContext is NULL!!");
    }catch (Exception e) {
      e.printStackTrace();
    }
    
    }
 */
  
}
