/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

// java.util classes
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.core.cluster.Cluster;

import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogicProvider;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.SubscriptionClient;

// ClusterIdentification
import org.cougaar.core.cluster.ClusterIdentifier;

// root = ClusterMessage
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.society.Message;
// inherits from ClusterMessage
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.core.cluster.AckDirectiveMessage;
// inherits from Directive; Directive inherits from ClusterMessage
//import org.cougaar.domain.planning.ldm.plan.Asset;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;
import org.cougaar.domain.planning.ldm.plan.Notification;
import org.cougaar.domain.planning.ldm.plan.Task;

import org.cougaar.core.plugin.AddPlugInMessage;
import org.cougaar.core.plugin.RemovePlugInMessage;

// new LDM plugins
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.plugin.LDMPlugInServesLDM;
import org.cougaar.core.plugin.ScheduleablePlugIn;
import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.core.plugin.ParameterizedPlugIn;
import org.cougaar.core.plugin.PlugIn;
import org.cougaar.core.plugin.StatelessPlugInAdapter;

//
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.Domain;
import org.cougaar.domain.planning.ldm.DomainManager;

// Advertised services I provide to my collaborators
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.StateModelException;
import org.cougaar.core.cluster.ClusterStateModel;
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.cluster.NoResponseException;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.ClusterServesMessageTransport;

// Services I require from my collaborators
import org.cougaar.core.society.ClusterManagementServesCluster;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.domain.planning.ldm.Registry;
import org.cougaar.domain.planning.ldm.RegistryException;

// ClusterFactories
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

// cluster context registration
import org.cougaar.core.cluster.ClusterContext;

import java.io.*;

import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactoryImpl;

// alternate messenger service
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.core.society.MessageTransportWatcher;

// Scenario time support
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.AdvanceClockMessage;

// Persistence
import org.cougaar.core.cluster.persist.BasePersistence;
import org.cougaar.core.cluster.persist.DatabasePersistence;
import org.cougaar.core.cluster.persist.Persistence;
import org.cougaar.core.cluster.persist.PersistenceException;

import java.beans.Beans;

/**
 * ClusterObject is designed specifically for the December
 * 1997 Workshop.  The inention is to create a temporary place holder
 * for ClusterManagement until more mature implementations of ClusterManagement
 * emerges.  The choice of making the class final is intended to confirm
 * that this class is NOT the generalization of ClusterManagement behaviors, but
 * rather, a concrete prototype.
 *
 * System properties:
 * org.cougaar.core.cluster.heartbeat : a low-priority thread runs and prints
 *  a '.' every few seconds when nothing else much is going on.
 *  This is a one-per-vm function.  Default true.
 * org.cougaar.core.cluster.metrics : should message traffic metrics be measures and metrics info
 *  be sent to an external metrics display.
 * org.cougaar.core.cluster.metricsInterval : milliseconds between sending of metrics info to
 *  metrics display.  If <= 0, no such messages will be sent.
 * org.cougaar.core.cluster.idleInterval : how long between idle detection and heartbeat cycles (prints '.');
 * org.cougaar.core.cluster.idle.verbose : if true, will print elapsed time (seconds) since
 *   cluster start every idle.interval millis.
 * org.cougaar.core.cluster.idle.verbose.interval=60000 : millis between verbose idle reports
 * org.cougaar.core.cluster.showTraffic : if True, shows '+' and '-' on message sends and receives.  if
 *  false, also turns off reports of heartbeat ('.') and other status chars.
 * org.cougaar.core.cluster.trafficPeriod : how many traffic characters to print before LF.
 *
 * @see org.cougaar.core.society.MessageTransport
 **/
public class ClusterImpl
  implements Cluster, LDMServesPlugIn, ClusterContext, MessageTransportClient, MessageStatistics
{
  private Distributor myDistributor = null;

  private ALPPlan myALPPlan = null;
  private LogPlan myLogPlan = null;

  private MessageTransportServer messenger = null;

  private static boolean isHeartbeatOn = true;

  private static boolean isMetricsHeartbeatOn = false;

  private static int metricsInterval = 2500; // how often send to metrics display
  private static int idleInterval = 5*1000;

  private static boolean idleVerbose = false; // don't be verbose
  private static long idleVerboseInterval = 60*1000L; // 1 minute

  private static long maxIdleInterval;

  private static int trafficPeriod = 80;
  private static boolean usePlugInLoader = false;

  private static boolean showTraffic = true;
  static {
    Properties props = System.getProperties();
    isHeartbeatOn=(Boolean.valueOf(props.getProperty("org.cougaar.core.cluster.heartbeat", "true"))). booleanValue();
    isMetricsHeartbeatOn=(Boolean.valueOf(props.getProperty("org.cougaar.core.cluster.metrics", "false"))).booleanValue();
    usePlugInLoader=(Boolean.valueOf(props.getProperty("org.cougaar.core.cluster.pluginloader", "false"))).booleanValue();
    metricsInterval=(Integer.valueOf(props.getProperty("org.cougaar.core.cluster.metricsInterval", "2500"))).intValue();
    idleInterval=(Integer.valueOf(props.getProperty("org.cougaar.core.cluster.idleInterval", "5000"))).intValue();
    maxIdleInterval = (idleInterval+(idleInterval/10));

    showTraffic=(Boolean.valueOf(props.getProperty("org.cougaar.core.cluster.showTraffic", "true"))).booleanValue();
    trafficPeriod=(Integer.valueOf(props.getProperty("org.cougaar.core.cluster.trafficPeriod", "80"))).intValue();
    trafficPeriod--; // 0 origin, so if the variable is <0, no output

    idleVerbose = (Boolean.valueOf(props.getProperty("org.cougaar.core.cluster.idle.verbose", "true"))).booleanValue();
    //idleVerbose =true;
    idleVerboseInterval = (Integer.valueOf(props.getProperty("org.cougaar.core.cluster.idle.verbose.interval", "60000"))).intValue();
  }


  /**
   * myClusterManager_ is a private reference to this cluster's
   * ClusterManagement container.  This value is set once and only
   * once during the #load(Object) phase.
   **/
  ClusterManagementServesCluster myClusterManager_;
        
  /** 
   * Answer with the reference to the ClusterManagement instance
   * that manages me.
   * @return Object the enclosing ClusterManagement instance
   **/
  private ClusterManagementServesCluster getClusterManagement() {
    return myClusterManager_;
  }
    
  private void setClusterManagement(ClusterManagementServesCluster myClusterManagement){
    myClusterManager_ = myClusterManagement;
  }
  /**
   * myClusterIdentity_ is a private representation of this instance's
   * ClusterIdentifier.
   **/
  private ClusterIdentifier myClusterIdentity_;
    
  /**
   * myClusterStateModelState_ is the private representation of this
   * instance's state.
   **/
  private int myClusterStateModelState_ = GenericStateModel.UNINITIALIZED;
    
    
  /** the cluster's UIDServer **/
  private UIDServer myUIDServer = null;

  /**
   * Privately set ClusterStateModelState.  Raise an
   * StateModelException if the transition is invalid.
   **/
     
  private void setState ( int newState ) throws StateModelException {
    myClusterStateModelState_ = newState;
  }

  protected Persistence createPersistence() {
    if (System.getProperty("org.cougaar.core.cluster.persistence.enable", "false").equals("false"))
      return null;		// Disable persistence for now
    try {
      Persistence result = BasePersistence.find(this);
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
    
  public Distributor getDistributor() {
    //if (myDistributor != null) return myDistributor;
    synchronized (this) {
      if (myDistributor == null) {
        myDistributor = new Distributor(getClusterIdentifier().getAddress());
        Persistence persistence = createPersistence();
        myDistributor.setPersistence(persistence);
        myDistributor.start(this);
      }
      return myDistributor;
    }
  }

  /** 
   * @return the cluster's ConfigFinder.
   **/
  public ConfigFinder getConfigFinder() {
    return ConfigFinder.getInstance();
  }

  /**
   * Answer with a String representation of yourself.
   * @return String String representation of this instance.
   */
  public String toString()
  {
    String body = "anonymous";
    ClusterIdentifier cid = getClusterIdentifier();
    if (cid != null)
      body = cid.getAddress();
    return "<Cluster " + body + ">";
  }

    
  /** Initialize.  Transition object from undefined to INITIALIZED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void initialize() throws StateModelException {
    setState(GenericStateModel.UNLOADED);
  }
    

  /** Notify object about its "parent"
   *  Object should transition to the LOADED state.
   *  Called object should check that caller is an instanceof
   *  the appropriate class
   *  @param o the "parent" object of the object being loaded
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void load(Object o) throws StateModelException {
    // Confirm that my container is indeed ClusterManagement
    if (!( o instanceof ClusterManagementServesCluster ) ) {
      throw new StateModelException ("Container does not implement ClusterManagementServesCluster");
    }

    ClusterManagementServesCluster cm = (ClusterManagementServesCluster) o;
    setClusterManagement(cm);

    myUIDServer = new UIDServer(this);

    // get the Messenger instance from ClusterManagement
    messenger = cm.getMessageTransportServer();

    // add ourselves to our VM's cluster table
    ClusterContextTable.addContext(getClusterIdentifier(), this);

    // initialize LDM parts
    myRegistry = new Registry();

    // get the domains set up
    DomainManager.initialize();

    // set up the root domain, especially the root factory.
    Domain rootDomain = DomainManager.find("root");
    myRootFactory = (RootFactory) rootDomain.getFactory(this);

    // set up ALPPlan and LogicProviders
    try {
      myALPPlan = new ALPPlan(getDistributor(), this);
      getDistributor().setALPPlan(myALPPlan);

      Collection domains = DomainManager.values();
      for (Iterator i = domains.iterator(); i.hasNext(); ) {
        Domain d = (Domain) i.next();

        // add all the domain-specific logic providers
        XPlanServesALPPlan xPlan = d.createXPlan(myALPPlan.getXPlans());
        myALPPlan.addXPlan(xPlan);
        if (d == rootDomain) {
          myLogPlan = (LogPlan) xPlan;
        }
        Collection lps = d.createLogicProviders(xPlan, this);
        if (lps != null) {
          for (Iterator li = lps.iterator(); li.hasNext(); ) {
            Object lo = li.next();
            if (lo instanceof LogicProvider) {
              myALPPlan.addLogicProvider((LogicProvider) lo);
            } else {
              System.err.println("Domain "+d+" requested loading of a non LogicProvider "+lo+" (Ignored).");
            }
          }
        }
      }

      // specialLPs
      if (isMetricsHeartbeatOn) {
        myALPPlan.addLogicProvider(new MetricsLP(myLogPlan, this));
      }
      myALPPlan.init();

    } catch (Exception e) { 
      synchronized (System.err) {
        System.err.println("Problem loading LogPlan: ");
        e.printStackTrace(); 
      }
    }

    // set up all the factories
    Collection keys = DomainManager.keySet();
    for (Iterator i = keys.iterator(); i.hasNext(); ) {
      String key = (String) i.next();
      try {
        getFactory(key);
      } catch (Exception e) { 
        synchronized (System.err) {
          System.err.println("Problem loading Domain Factory \""+key+"\": ");
          e.printStackTrace(); 
        }
      }
    }

    // transit the state.
    setState(GenericStateModel.LOADED);
  }

  /** Called object should start any threads it requires.
   *  Called object should transition to the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void start() throws StateModelException {

    // activate the metrics watcher before we register
    if (isMetricsHeartbeatOn) getMessageWatcher();

    messenger.registerClient(this);

    if (isHeartbeatOn) {
      startHeartbeat();
    }

    if (isMetricsHeartbeatOn) {
      startMetricsHeartbeat();
      metricsOn = true;
    }

    setState(GenericStateModel.ACTIVE);
  }

  /** Called object should pause operations in such a way that they may
   *  be cleanly resumed or the object can be unloaded.
   *  Called object should transition from the ACTIVE state to
   *  the IDLE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void suspend() throws StateModelException {
    setState(GenericStateModel.IDLE);
    
  }

  /** Called object should transition from the IDLE state back to
   *  the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void resume() throws StateModelException {
    setState(GenericStateModel.ACTIVE);
  }

  /** Called object should transition from the IDLE state
   *  to the LOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void stop() throws StateModelException {
    setState(GenericStateModel.LOADED);
  }

  /**  Called object should transition from ACTIVE or SERVING state
   *   to the LOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void halt() throws StateModelException {
    setState(GenericStateModel.LOADED);
  }

  /** Called object should perform any cleanup operations and transition
   *  to the UNLOADED state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void unload() throws StateModelException {
    setState(GenericStateModel.UNLOADED);
  }


  /** Return the current state of the object: LOADED, UNLOADED, 
   * ACTIVE, or IDLE.
   * @return object state
   **/

  public int getState() { return myClusterStateModelState_; }

  /** Standard, no argument constructor. */
  public ClusterImpl() {
  }
  
  /** Answer by allowing ClusterManagement to set the ClusterIdentifier for this instance.
   * Assert that this is called once and only once *before* ClusterManagement calls
   * initialize.
   **/
  public void setClusterIdentifier( ClusterIdentifier aClusterIdentifier ) {
    if ( myClusterIdentity_ != null )
      throw new RuntimeException ("Attempt to over-ride ClusterIdentity detected.");
    myClusterIdentity_ = aClusterIdentifier;
  }
    
  ///
  /// ClusterServesMessageTransport
  ///

  public String getIdentifier() {
    return myClusterIdentity_.getAddress();
  }

  public MessageAddress getMessageAddress() {
    return myClusterIdentity_;
  }

  // 
  // MessageTransportClient
  //
  public void receiveMessage(Message message) {
    showProgress("-");

    try {
      if (message instanceof AdvanceClockMessage) {
        handleAdvanceClockMessage((AdvanceClockMessage)message);
      } else if (message instanceof ClusterMessage) {
        if (message instanceof AddPlugInMessage) {
          receiveAddPlugInMessage((AddPlugInMessage)message);
        } else if (message instanceof RemovePlugInMessage ){
          receiveRemovePlugInMessage((RemovePlugInMessage) message );
        } else if (message instanceof ClusterInitializedMessage ) {
          receiveClusterInitializedMessage((ClusterInitializedMessage) message);
        } else {
          // internal message queue
          getQueueHandler().addMessage((ClusterMessage)message);
        }
      } else {
        System.err.println("\n"+this+": Received unhandled Message: "+message);
      }
    } catch( Exception e) {
      System.err.println("Unhandled Exception: "+e);
      e.printStackTrace();
    }
  }

  void receiveClusterInitializedMessage(ClusterInitializedMessage cim) {
    startQueueHandler();
  }


  /** Receiver for in-band messages (queued by QueueHandler)
   */
  void receiveQueuedMessages(List messages) {
    myDistributor.receiveMessages(messages);
  }

  //
  // MessageStatistics
  //

  public MessageStatistics.Statistics getMessageStatistics(boolean reset) {
    if (messenger instanceof MessageStatistics) {
      return ((MessageStatistics) messenger).getMessageStatistics(reset);
    } else {
      return null;
    }
  }

  // 
  // LDM functionality
  //


  /** set of PrototypeProvider LDM PlugIns **/
  // might want this to be prioritized lists
  private List prototypeProviders = new ArrayList();

  public void addPrototypeProvider(PrototypeProvider prov) {
    prototypeProviders.add(prov);
  }

  /** set of PropertyProvider LDM PlugIns **/
  private List propertyProviders = new ArrayList();
  public void addPropertyProvider(PropertyProvider prov) {
    propertyProviders.add(prov);
  }

  // use the registry for registering prototypes for now.
  // later, just replace with a hash table.
  public void cachePrototype(String aTypeName, Asset aPrototype) {
    Registry r = getRegistry();
    r.createRegistryTerm(aTypeName, aPrototype);
  }

  public boolean isPrototypeCached(String aTypeName) {
    return (getRegistry().findDomainName(aTypeName) != null);
  }    

  public Asset getPrototype(String aTypeName) {
    return getPrototype(aTypeName, null);
  }
  public Asset getPrototype(String aTypeName, Class anAssetClass) {
    Asset found = null;

    // look in our registry first.
    // the catch is in case some bozo registered a non-asset under this
    // name.
    try {
      found = (Asset) getRegistry().findDomainName(aTypeName);
      if (found != null) return found;
    } catch (ClassCastException cce) {}
    
    // else, try the prototype providers
    for (Iterator pps = prototypeProviders.iterator(); pps.hasNext(); ) {
      PrototypeProvider pp = (PrototypeProvider) pps.next();
      found = pp.getPrototype(aTypeName, anAssetClass);
      if (found != null) return found;
    }

    // might want to throw an exception in a later version
    return null;
  }

  public void fillProperties(Asset anAsset) {
    // expose the asset to all propertyproviders
    for (Iterator pps = propertyProviders.iterator(); pps.hasNext(); ) {
      PropertyProvider pp = (PropertyProvider) pps.next();
      pp.fillProperties(anAsset);
    }
  }
        

  /** hash of PropertyGroup interface to Lists of LatePropertyProvider instances. **/
  private HashMap latePPs = new HashMap(11);
  /** list of LatePropertyProviders who supply all PropertyGroups **/
  private ArrayList defaultLatePPs = new ArrayList(3); 
  public void addLatePropertyProvider(LatePropertyProvider lpp) {
    Collection c = lpp.getPropertyGroupsProvided();
    if (c == null) {
      defaultLatePPs.add(lpp);
    } else {
      try {
        for (Iterator it = c.iterator(); it.hasNext(); ) {
          Class pgc = (Class) it.next();
          ArrayList l = (ArrayList) latePPs.get(pgc);
          if (l == null) {
            l = new ArrayList(3);
            latePPs.put(pgc,l);
          }
          l.add(lpp);
        }
      } catch (ClassCastException e) {
        System.err.println("LatePropertyProvider "+lpp+" returned an illegal PropertyGroup spec:");
        e.printStackTrace();
      }
    }
  }

  /** hook for late-binding **/
  public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pgclass, long t) {
    // specifics
    ArrayList c = (ArrayList) latePPs.get(pgclass);
    PropertyGroup pg = null;
    if (c != null) {
      pg = tryLateFillers(c, anAsset, pgclass, t);
    }
    if (pg == null) {
      pg = tryLateFillers(defaultLatePPs, anAsset, pgclass, t);
    }
    return pg;
  }

  /** utility method of lateFillPropertyGroup() **/
  private PropertyGroup tryLateFillers(ArrayList c, Asset anAsset, Class pgclass, long t)
  {
    int l = c.size();
    for (int i = 0; i<l; i++) {
      LatePropertyProvider lpp = (LatePropertyProvider) c.get(i);
      PropertyGroup pg = lpp.fillPropertyGroup(anAsset, pgclass, t);
      if (pg != null) 
        return pg;
    }
    return null;
  }    


  /** 
   * Answer with a reference to the Factory
   * It is inteded that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  public ClusterObjectFactory getClusterObjectFactory()
  {
    return myRootFactory;
  }

  private RootFactory myRootFactory = null;
  private Registry myRegistry = null;

  /** expose the LDM factory instance to consumers.
   *    @return LdmFactory The fatory object to use in constructing LDM Objects
   **/
  public RootFactory getFactory(){
    return myRootFactory;
  }

  /** map of domainname to domain factory instance **/
  private HashMap factories = new HashMap(11);

  /** @deprecated use getFactory() **/
  public RootFactory getLdmFactory() {
    return getFactory();
  }

  /** create a domain-specific factory **/
  public Factory getFactory(String domainname) {
    String key = domainname;
    synchronized (factories) {
      Factory f = (Factory) factories.get(key);
      if (f != null) return f;

      // bail out for root
      if ("root".equals(domainname)) {
        factories.put(key, myRootFactory);
        return myRootFactory;
      }
        
      Domain d = DomainManager.find(key);
      if (d == null) return null; // couldn't find the domain!
      f = d.getFactory(this);   // create a new factory
      if (f == null) return null; // failed to create the factory
      factories.put(key, f);
      return f;
    }
  }

  /** Expose the Registry to consumers. 
   **/
  public Registry getRegistry() {
    return myRegistry;
  }


  // bean instantiation
  public Object instantiateBean(String aBeanAsString) throws ClassNotFoundException {
    return getClusterManagement().instantiateBean(aBeanAsString);
  }

  public Object instantiateBean(ClassLoader classloader, String aBeanAsString) 
    throws ClassNotFoundException 
  {
    return getClusterManagement().instantiateBean(classloader, aBeanAsString);
  }

  /**
   *   This method is resposible for accepting any Object for logging and passing it to
   *   the LogWriter.
   *   <p><PRE>
   *   PRE CONDITION:    Log Writer created and running under its own thread
   *   POST CONDITION:   Object passed to the LogWriter Thread
   *   INVARIANCE:
   *   </PRE>
   *   @param Object The object to write to the log file
   **/
  public void logEvent( Object anEvent )
  {
    getClusterManagement().logEvent(anEvent);
  }

  public void sendMessage(ClusterMessage message)
  {
    showProgress("+");
    try {
      if (messenger == null) {
        throw new RuntimeException("MessageTransport unavailable. Message dropped.");
      } 
      messenger.sendMessage(message);
    } catch ( Exception ex ) {
      synchronized (System.err) {
        System.err.println("Problem sending message: ");
        ex.printStackTrace();
      }
    }
  }

  public UIDServer getUIDServer() {
    return myUIDServer;
  }

  // additional ClusterContext implementation

  /** Answer with your represenation as a ClusterIdentifier instance. **/
  public ClusterIdentifier getClusterIdentifier() 
  { 
    return myClusterIdentity_;
  }

  public LDMServesPlugIn getLDM() {
    return this;
  }

  public void schedulePlugIn(ScheduleablePlugIn plugin) {
    getSharedPlugInManager().registerPlugIn(plugin);
  }

  private SharedPlugInManager _sharedPlugInManager = null;
  private SharedPlugInManager getSharedPlugInManager() {
    synchronized (this) {
      if (_sharedPlugInManager == null) {
        _sharedPlugInManager = new SharedPlugInManager(getClusterIdentifier());
      }
      return _sharedPlugInManager;
    }
  }

  public MetricsSnapshot getMetricsSnapshot() {
    MetricsSnapshot ms = new MetricsSnapshot();
    ms.clusterName = getClusterIdentifier().cleanToString();
    ms.nodeName = getClusterManagement().getName();
    ms.time = System.currentTimeMillis();

    // message transport stuff
    MessageWatcher mw = getMessageWatcher();
    ms.directivesIn = mw.directivesIn;
    ms.directivesOut = mw.directivesOut;
    ms.notificationsIn = mw.notificationsIn;
    ms.notificationsOut = mw.notificationsOut;

    // logplan stuff
    ms.assets = myLogPlan.getAssetCount();
    ms.planelements = myLogPlan.getPlanElementCount();
    ms.tasks = myLogPlan.getTaskCount();
    ms.workflows = myLogPlan.getWorkflowCount();

    // cluster metrics
    ms.pluginCount = plugins.size();
    ms.thinPluginCount = getSharedPlugInManager().size();
    ms.prototypeProviderCount = prototypeProviders.size();
    ms.propertyProviderCount = propertyProviders.size();
    ms.cachedPrototypeCount = getRegistry().size();

    // vm stuff
    ms.idleTime = getIdleTime();
    ms.freeMemory = Runtime.getRuntime().freeMemory();
    ms.totalMemory = Runtime.getRuntime().totalMemory();
    ms.threadCount = Thread.currentThread().getThreadGroup().activeCount();

    return ms;
  }

  public ClassLoader getLDMClassLoader() {
    if (usePlugInLoader) {
      return PlugInLoader.getInstance().getClassLoader();
    } else {
      ClassLoader cl = this.getClass().getClassLoader();
      if (cl == null) {
        cl = ClassLoader.getSystemClassLoader();
      }
      return cl;
    }
  }

  private static final HashMap purePlugIns = new HashMap(11);
  private static PlugIn getPurePlugIn(Class c) {
    synchronized (purePlugIns) {
      PlugIn plugin = (PlugIn)purePlugIns.get(c);
      if (plugin == null) {
        try {
          plugin = (PlugIn) c.newInstance();
          purePlugIns.put(c, plugin);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return plugin;
    }
  }

  public void receiveAddPlugInMessage ( AddPlugInMessage message ) {
    String pluginclass = message.getPlugIn();
    try {
      Vector args = message.getArguments();
      PlugInServesCluster newPlugIn;

      // get the class of the plugin
      Class pc = Class.forName(pluginclass);
      if (PlugIn.class.isAssignableFrom(pc)) {
        // is a stateless plugin
        newPlugIn = new StatelessPlugInAdapter(getPurePlugIn(pc));
      } else {
        if (usePlugInLoader) {
          PlugInLoader pil = PlugInLoader.getInstance();
          newPlugIn = pil.instantiatePlugIn(pluginclass, args);
        } else {
          newPlugIn = (PlugInServesCluster)Beans.instantiate(getClass().getClassLoader(), pluginclass);
        }
      }

      if (newPlugIn instanceof ParameterizedPlugIn) {
        if (args != null)
          ((ParameterizedPlugIn)newPlugIn).setParameters(args);
      }

      if (newPlugIn == null) 
        throw new RuntimeException("Could not instantiate "+pluginclass);

      newPlugIn.initialize();
      newPlugIn.load( this );
      newPlugIn.start();
      
      // make sure that the plugin started (and is active)
      if( newPlugIn.getState() != PlugInServesCluster.ACTIVE)
        throw new java.lang.IllegalArgumentException("PlugIn was not in ACTIVE state");

      // record it
      addPlugIn(newPlugIn);

      // Handle LDM plugins
      if (newPlugIn instanceof PrototypeProvider) {
          addPrototypeProvider((PrototypeProvider)newPlugIn);
      }

      if (newPlugIn instanceof PropertyProvider) {
          addPropertyProvider((PropertyProvider)newPlugIn);
      }
      if (newPlugIn instanceof LatePropertyProvider) {
          addLatePropertyProvider((LatePropertyProvider)newPlugIn);
      }

    } catch(Throwable ex) {
      synchronized (System.err) {
        System.err.println("Failed to load "+pluginclass+":\n"+ex);
        if (!(ex instanceof ClassNotFoundException))
          ex.printStackTrace();
      }
    }
  }
 
  public void receiveRemovePlugInMessage ( RemovePlugInMessage message ) {
    String theClassName = message.getPlugIn();
    System.err.println("RemovePlugin Message is disabled: "+theClassName);
  }
    
    
  ///
  /// Manage the plugins
  ///
  private List plugins = new ArrayList();

  public List getPlugins() {
    return plugins;
  }
  public void addPlugIn(PlugInServesCluster plugin) {
    plugins.add(plugin);
  }
  private boolean removePlugIn(PlugInServesCluster plugin) {
    return plugins.remove(plugin);
  }
  private boolean containsPlugIn(PlugInServesCluster plugin) {
    return plugins.contains(plugin);
  }


  //
  // Queue Handler
  //

  private QueueHandler queueHandler = null;

  private boolean isQueueHandlerStarted = false;

  private void startQueueHandler() {
    if (! isQueueHandlerStarted ) {
      getQueueHandler().start();
      isQueueHandlerStarted = true;
    } else {
      System.err.println("QueueHandler in " + getClusterIdentifier() + " asked to restart.");
    }
  }

  private QueueHandler getQueueHandler() {
    synchronized (this) {
      if (queueHandler == null) {
        queueHandler =  new QueueHandler(getClusterIdentifier().getAddress() + "/RQ");
      }
      return queueHandler;
    }
  }

  private class QueueHandler extends Thread {
    private List queue = new ArrayList();
    private List msgs = new ArrayList();
    public QueueHandler(String name) {
      super(name);
    }
    public void run() {
      ClusterMessage m;
      int size;
      while (true) {
        synchronized (queue) {
          while (queue.isEmpty()) {
            try {
              queue.wait();
            }
            catch (InterruptedException ie) {
            }
          }
          msgs.addAll(queue);
          queue.clear();
        }
        receiveQueuedMessages(msgs);
        msgs.clear();
      }
    }

    public void addMessage(ClusterMessage m) {
      synchronized (queue) {
        queue.add(m);
        queue.notify();
      }
    }
  }

  /** implement a low-priority heartbeat function which
   * just prints '.'s every few seconds when nothing else
   * is happening.
   * deactivated by -Dalp.cluster.heartbeat=false
   **/
  private class Heartbeat implements Runnable {
    private long firstTime;
    private long lastVerboseTime;

    public Heartbeat() {
      firstTime = System.currentTimeMillis();
      lastVerboseTime = firstTime;
    }

    public void run() {
      // if heartbeat actually gets to run at least every 5.5 seconds,
      // we'll consider the VM idle.
      while (true) {
        try {
          Thread.sleep(idleInterval); // sleep for (at least) 5s
        } catch (InterruptedException ie) {}
        showProgress(".");
        long t = System.currentTimeMillis();
        if (lastHeartbeat!=0) {
          long delta = t-lastHeartbeat;
          if (delta <= maxIdleInterval) {
            // we're pretty much idle
            idleTime += delta;
          } else {
            idleTime = 0;
          }
        }
        lastHeartbeat = t;

        if (idleVerbose) {
          long delta = t-lastVerboseTime;
          if (delta >= idleVerboseInterval) {
            showProgress("("+Long.toString(((t-firstTime)+500)/1000)+")");
            lastVerboseTime=t;
          }
        }
      }
    }
  }

  private static Thread heartbeat = null;
  private static Object heartbeatLock = new Object();

  private static long lastHeartbeat = 0L;
  private static long idleTime = 0L;

  private void startHeartbeat() {
    synchronized (heartbeatLock) {
      if (heartbeat == null) {
        heartbeat = new Thread(new Heartbeat(), "Heartbeat");
        heartbeat.setPriority(Thread.MIN_PRIORITY);
        heartbeat.start();
      }
    }
  }

  public static long getIdleTime() { 
    long delta = System.currentTimeMillis() - lastHeartbeat;
    if (delta <= maxIdleInterval) {
      return idleTime+delta;
    } else {
      return 0;
    }
  }


  /** Send a status to metrics display every n seconds
   * consisting of a count of messages in/out and a 
   * count of the logplan elements.  Only if org.cougaar.core.cluster.metrics is true.
   **/
  private class MetricsHeartbeat implements Runnable {
    public void run() {
      boolean checked = false;
      boolean keepRunning = true;
      boolean isAddressKnown = false;
      while (keepRunning) {
        try {
          Thread.sleep(metricsInterval); // sleep for (at least) this time
        } catch (InterruptedException ie) {}
        if (messenger!=null) {
          if (checked || messenger.addressKnown(metricsAddress)) {
            checked=true;
            // Send metrics info.  Get logplan cnts from logplan, pass it along
            int assetcnt = myLogPlan.getAssetCount();
            int planelemcnt = myLogPlan.getPlanElementCount();
            int taskcnt = myLogPlan.getTaskCount();
            int workflowcnt = myLogPlan.getWorkflowCount();
            // turn off metrics heartbeat after first failed metrics message
          
            MessageWatcher mw = getMessageWatcher();
            Message mm = new MetricsMessage(getMessageAddress(), metricsAddress,
                                            System.currentTimeMillis(),
                                            mw.directivesIn, mw.directivesOut,
                                            mw.notificationsIn, mw.notificationsOut,
                                            assetcnt, planelemcnt,
                                            taskcnt, workflowcnt);
            messenger.sendMessage(mm);
          } else {
	    isMetricsHeartbeatOn=false;
	    keepRunning = false;
	  }
        }
      }
    }
  }

  public static MessageAddress metricsAddress = new MessageAddress("metrics");

  protected MessageWatcher _messageWatcher = null;

  private MessageWatcher getMessageWatcher() {
    if (_messageWatcher == null)
      messenger.addMessageTransportWatcher(_messageWatcher = new MessageWatcher());
    return _messageWatcher;
  }

  class MessageWatcher implements MessageTransportWatcher {
    MessageAddress me;

    int directivesIn = 0;
    int directivesOut = 0;
    int notificationsIn = 0;
    int notificationsOut = 0;
    
    public MessageWatcher() {
      me = getClusterIdentifier();
    }

    public void messageSent(Message m) {
      if (m.getOriginator().equals(me)) {
        if (m instanceof DirectiveMessage) {
          Directive[] directives = ((DirectiveMessage)m).getDirectives();
          for (int i = 0; i < directives.length; i++) {
            if (directives[i] instanceof Notification)
              notificationsOut++;
            else
              directivesOut++;
          }
        }
      }
    }
    public void messageReceived(Message m) {
      if (m.getTarget().equals(me)) {
        if (m instanceof DirectiveMessage) {
          Directive[] directives = ((DirectiveMessage)m).getDirectives();
          for (int i = 0; i < directives.length; i++) {
            if (directives[i] instanceof Notification)
              notificationsIn++;
            else
              directivesIn++;
          }
        }
      }
    }
  }

  private Thread metricsheartbeat = null;

  private void startMetricsHeartbeat() {
    if (metricsheartbeat == null) {
      metricsheartbeat = new Thread(new MetricsHeartbeat(), "MetricsHeartbeat");
      metricsheartbeat.setPriority(Thread.MAX_PRIORITY);
      metricsheartbeat.start();
    }
  }

  //
  // ALP Scenario Time management and support for PlugIns
  //

  // one timer per vm - conserve threads.
  static ExecutionTimer _xTimer;
  static Timer _rTimer;
  static {
    _xTimer = new ExecutionTimer();
    _xTimer.start();
    _rTimer = new RealTimer();
    _rTimer.start();
  }

  /** Set the Scenario time to some specific time (stopped).
   *
   * Equivalent to setTime(time, false);
   * 
   **/
  public void setTime(long time) {
    sendAdvanceClockMessage(time, true, 0.0, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }

 /**
  * This method sets the ALP scenario time to a specific time
  * in the future.  
  * @param time milliseconds in java time.
  * @param running should the clock continue to run after setting the time?
  **/
  public void setTime(long time, boolean running) {
    sendAdvanceClockMessage(time, true, 0.0, running, _xTimer.DEFAULT_CHANGE_DELAY);
  }
	
 /**
  * This method sets the ALP scenario time to a specific rate.
  * @param newRate the new rate. Execution time advance at the new rate after a brief delay
  **/
  public void setTimeRate(double newRate) {
    sendAdvanceClockMessage(0L, false, newRate, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }
	
  /**
   * This method advances the ALP scenario time a period of time
   * in the future, leaving the clock stopped.
   * equivalent to advanceTime(timePeriod, false);
   **/
  public void advanceTime(long timePeriod){
    sendAdvanceClockMessage(timePeriod, false, 0.0, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }

  /**
   * This method advances the ALP scenario time a period of time
   * in the future.
   * @param timePeriod Milliseconds to advance the scenario clock.
   * @param running should the clock continue to run after setting.
   **/
  public void advanceTime(long timePeriod, boolean running){
    sendAdvanceClockMessage(timePeriod, false, 0.0, running, _xTimer.DEFAULT_CHANGE_DELAY);
  }

  /**
   * This method advances the ALP scenario time a period of time
   * leaving the clock running at a new rate.
   * @param timePeriod Milliseconds to advance the scenario clock.
   * @param newRate the new rate
   **/
  public void advanceTime(long timePeriod, double newRate){
    sendAdvanceClockMessage(timePeriod, false, newRate, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }

  public void advanceTime(ExecutionTimer.Change[] changes) {
    ExecutionTimer.Parameters[] params = _xTimer.create(changes);
    for (int i = 0; i < params.length; i++) {
      sendAdvanceClockMessage(params[i]);
    }
  }

  public double getExecutionRate() {
    return _xTimer.getRate();
  }

  private void sendAdvanceClockMessage(long millis,
                                       boolean millisIsAbsolute,
                                       double newRate,
                                       boolean forceRunning,
                                       long changeDelay)
  {
    MessageAddress a = getClusterIdentifier();
    ExecutionTimer.Parameters newParameters =
      _xTimer.create(millis, millisIsAbsolute, newRate, forceRunning, changeDelay);
    Message m = new AdvanceClockMessage(a, newParameters);
    messenger.sendMessage(m);
  }

  private void sendAdvanceClockMessage(ExecutionTimer.Parameters newParameters) {
    MessageAddress a = getClusterIdentifier();
    Message m = new AdvanceClockMessage(a, newParameters);
    messenger.sendMessage(m);
  }

  /**
   * This method gets the current ALP scenario time. 
   * The returned time is in milliseconds.
   **/
  public long currentTimeMillis( ){
    return _xTimer.currentTimeMillis();
  }

  public void addAlarm(Alarm alarm) {
    _xTimer.addAlarm(alarm);
  }

  public void handleAdvanceClockMessage(AdvanceClockMessage acm) {
    _xTimer.setParameters(acm.getParameters());
  }

  /* RealTime Alarms */
  public void addRealTimeAlarm(Alarm alarm) {
    _rTimer.addAlarm(alarm);
  }

  ///////////////////////////////

  /*******************************************
   * Methods Added for Society Mangement Piece for MB 5.1.2
   ********************************************/
  public MetricsMessage getMetrics() {
    int assetcnt = myLogPlan.getAssetCount();
    int planelemcnt = myLogPlan.getPlanElementCount();
    int taskcnt = myLogPlan.getTaskCount();
    int workflowcnt = myLogPlan.getWorkflowCount();
    MetricsMessage mm = null;
    if (metricsOn) {
      MessageWatcher mw = getMessageWatcher();
      mm = new MetricsMessage(null, null,
                              System.currentTimeMillis(),
                              mw.directivesIn, mw.directivesOut,
                              mw.notificationsIn, mw.notificationsOut,
                              assetcnt, planelemcnt,
                              taskcnt, workflowcnt);
    } else {
      mm = new MetricsMessage(null, null, System.currentTimeMillis(),0,0,0,0,0,0,0,0);
    }
    return mm;
  }

  public boolean isMetricsOn() {
    return metricsOn;
  }

  private boolean metricsOn = false;

  public void startMetrics()
  {
    getMessageWatcher();        // Create if necessary
    if (metricsheartbeat == null) {
      metricsheartbeat = new Thread(new MetricsHeartbeat(), "MetricsHeartbeat");
      metricsheartbeat.setPriority(Thread.MAX_PRIORITY);
      metricsheartbeat.start();
    }
    metricsOn = true;
  }
  
  public void stopMetrics()
  {
    metricsOn = false;
  }

  private static int _progressCount = 0;
  private static Object _progressLock = new Object();
  private static void showProgress(String p) {
    if (showTraffic) {
      // Too many threads in a multi-cluster node are printing progress 
      // at the same time and we don't really care about the newlines
      // so we'll drop the synchronized and live with the consequences.
      //synchronized (_progressLock) { body; }
      System.out.print(p);
      /*
      if (trafficPeriod>=0) { 
        if (_progressCount++ >= trafficPeriod) {
          _progressCount=0;
          System.out.println();
        }
      }
      */
    }
  }

  /**
   **/
  public java.sql.Connection getDatabaseConnection(Object locker) {
    Persistence persistence = myDistributor.getPersistence();
    if (persistence instanceof DatabasePersistence) {
      DatabasePersistence dbp = (DatabasePersistence) persistence;
      return dbp.getDatabaseConnection(locker);
    } else {
      return null;
    }
  }

  public void releaseDatabaseConnection(Object locker) {
    Persistence persistence = myDistributor.getPersistence();
    if (persistence instanceof DatabasePersistence) {
      DatabasePersistence dbp = (DatabasePersistence) persistence;
      dbp.releaseDatabaseConnection(locker);
    }
  }
}
