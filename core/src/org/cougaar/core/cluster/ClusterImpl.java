/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

// java.util classes
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentBindingSite;

import org.cougaar.core.cluster.Cluster;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;

import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.MetricsSnapshot;
import org.cougaar.core.cluster.ClusterServesLogicProvider;
import org.cougaar.core.cluster.LogicProvider;
import org.cougaar.core.cluster.Subscriber;

// ClusterIdentification
import org.cougaar.core.cluster.ClusterIdentifier;

// root = ClusterMessage
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.ComponentMessage;
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

import org.cougaar.core.plugin.PluginManager;
import org.cougaar.core.plugin.AddPlugInMessage;
import org.cougaar.core.plugin.RemovePlugInMessage;

// new LDM plugins
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.core.plugin.LDMPlugInServesLDM;
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
import org.cougaar.domain.planning.ldm.DomainService;
import org.cougaar.domain.planning.ldm.DomainServiceImpl;
import org.cougaar.domain.planning.ldm.DomainServiceProvider;

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
import org.cougaar.domain.planning.ldm.PrototypeRegistryServiceProvider;
import org.cougaar.domain.planning.ldm.PrototypeRegistryService;
import org.cougaar.domain.planning.ldm.PrototypeRegistry;

// ClusterFactories
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

// cluster context registration
import org.cougaar.core.cluster.ClusterContext;

import java.io.*;

import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactoryImpl;

// alternate messenger service
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;

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
public class ClusterImpl extends Agent
  implements Cluster, LDMServesPlugIn, ClusterContext, MessageTransportClient, MessageStatistics
{
  private Distributor myDistributor = null;
  private Blackboard myBlackboard = null;
  private LogPlan myLogPlan = null;
  private MessageTransportService messenger = null;
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


  private AgentBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    //System.err.println("ClusterImpl.setBindingSite("+bs+")");
    if (bs instanceof AgentBindingSite) {
      bindingSite = (AgentBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+"into "+bs);
    }
  }

  protected final AgentBindingSite getBindingSite() {
    return bindingSite;
  }

  //
  // services
  //

  // UID Service
  private UIDService myUIDService = null;
  /** @deprectated  Use getUIDService() **/
  public UIDServer getUIDServer() {
    return myUIDService;
  }
  // public for now for backwards compatability
  public final UIDService getUIDService() {
    return myUIDService;
  }

  //Prototype Service
  private PrototypeRegistryService myPrototypeService = null;
  protected final PrototypeRegistryService getPrototypeRegistryService() {
    return myPrototypeService;
  }

  //Domain/Factory Service
  private DomainService myDomainService = null;
  protected final DomainService getDomainService() {
    return myDomainService;
  }

  // end services

 /**
   * myClusterManager_ is a private reference to this cluster's
   * ClusterManagement container.  This value is set once and only
   * once during the #load(Object) phase.
   **/
  //ClusterManagementServesCluster myClusterManager_;
        
  /** 
   * Answer with the reference to the ClusterManagement instance
   * that manages me.
   * @return Object the enclosing ClusterManagement instance
   **/
//   private ClusterManagementServesCluster getClusterManagement() {
//     return myClusterManager_;
//   }
    
//   private void setClusterManagement(ClusterManagementServesCluster myClusterManagement){
//     myClusterManager_ = myClusterManagement;
//   }
  /**
   * myClusterIdentity_ is a private representation of this instance's
   * ClusterIdentifier.
   **/
  private ClusterIdentifier myClusterIdentity_;
    
  /** Container which manages the plugins **/
  private PluginManager pluginManager;

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

    
  /** Notify object about its "parent"
   *  Object should transition to the LOADED state.
   *  Called object should check that caller is an instanceof
   *  the appropriate class
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void load() throws StateModelException {
    //System.err.println("Cluster.load()");
    // Confirm that my container is indeed ClusterManagement
    if (!( getBindingSite() instanceof AgentBindingSite ) ) {
      throw new StateModelException ("Container ("+getBindingSite()+") does not implement AgentBindingSite");
    }

    //ClusterManagementServesCluster cm = (ClusterManagementServesCluster) getBindingSite();
    //setClusterManagement(cm);

    // get the Messenger instance from ClusterManagement
    messenger = getBindingSite().getMessageTransportServer();

    // add ourselves to our VM's cluster table
    ClusterContextTable.addContext(getClusterIdentifier(), this);

    //set up the UIDServer and UIDService
    UIDServiceImpl theUIDServer = new UIDServiceImpl(this);
    childServiceBroker.addService(UIDService.class, new UIDServiceProvider(theUIDServer));
    // for backwards compatability
    myUIDService = (UIDService) childServiceBroker.getService(this, UIDService.class, null);
    
    //set up the PrototypeRegistry and the PrototypeRegistryService
    PrototypeRegistry myPrototypeRegistry = new PrototypeRegistry();
    childServiceBroker.addService(PrototypeRegistryService.class, 
                            new PrototypeRegistryServiceProvider(myPrototypeRegistry));
    //for backwards compatability
    myPrototypeService = (PrototypeRegistryService) childServiceBroker.getService(
                                                     this, PrototypeRegistryService.class, null);

    //set up the DomainServiceImpl and the DomainService
    // DomainServiceImpl needs the PrototypeRegistryService
    //for now its in the form of this as LDMServesPlugin - should be changed!!!
    DomainServiceImpl myDSI = new DomainServiceImpl(this);
    childServiceBroker.addService(DomainService.class, new DomainServiceProvider(myDSI));
    //for backwards compatability
    myDomainService = (DomainService) childServiceBroker.getService(this, DomainService.class, null);

    // set up Blackboard and LogicProviders
    try {
      myBlackboard = new Blackboard(getDistributor(), this);
      getDistributor().setBlackboard(myBlackboard);

      Collection domains = DomainManager.values();
      Domain rootDomain = DomainManager.find("root"); // HACK to let Metrics see plan objects

      for (Iterator i = domains.iterator(); i.hasNext(); ) {
        Domain d = (Domain) i.next();

        // add all the domain-specific logic providers
        XPlanServesBlackboard xPlan = d.createXPlan(myBlackboard.getXPlans());
        myBlackboard.addXPlan(xPlan);
        if (d == rootDomain) { // Replace HACK to let Metrics count plan objects
          myLogPlan = (LogPlan) xPlan;
        }
        Collection lps = d.createLogicProviders(xPlan, this);
        if (lps != null) {
          for (Iterator li = lps.iterator(); li.hasNext(); ) {
            Object lo = li.next();
            if (lo instanceof LogicProvider) {
              myBlackboard.addLogicProvider((LogicProvider) lo);
            } else {
              System.err.println("Domain "+d+" requested loading of a non LogicProvider "+lo+" (Ignored).");
            }
          }
        }
      }

      // specialLPs
      if (isMetricsHeartbeatOn) {
        myBlackboard.addLogicProvider(new MetricsLP(myLogPlan, this));
      }
      myBlackboard.init();

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
    super.load();

    // start up the pluginManager component - should really itself be loaded
    // as an agent subcomponent.
    pluginManager = new PluginManager(this);
    //System.err.println("Added PluginManager to "+this);
    add(pluginManager);

    //System.err.println("Cluster.load() completed");
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

    super.start();
  }


  /** Standard, no argument constructor. */
  public ClusterImpl() {
  }
  public ClusterImpl(ComponentDescription comdesc) {
    super(comdesc);
    // services added in load()
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
      } else if (message instanceof ComponentMessage) {
        ComponentMessage cm = (ComponentMessage)message;
        ComponentDescription desc = cm.getComponentDescription();
        int operation = cm.getOperation();
        switch (operation) {
          case ComponentMessage.ADD:     
            pluginManager.add(desc);     
            break;
          case ComponentMessage.REMOVE:  
            pluginManager.remove(desc);  
            break;
          case ComponentMessage.SUSPEND:
          case ComponentMessage.RESUME:
          case ComponentMessage.RELOAD:
            // not implemented yet -- requires modifications to Container
          default:
            throw new UnsupportedOperationException(
              "Unsupported ComponentMessage: "+message);
        }
      } else if (message instanceof ClusterMessage) {
        if (message instanceof AddPlugInMessage) {
          pluginManager.add((AddPlugInMessage)message);
        } else if (message instanceof RemovePlugInMessage ){
          pluginManager.remove((RemovePlugInMessage) message );
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
  // LDM PrototypeRegistry backwards compatability
  //

  public void addPrototypeProvider(PrototypeProvider prov) {
    getPrototypeRegistryService().addPrototypeProvider(prov);
  }
  public void cachePrototype(String aTypeName, Asset aPrototype) {
    getPrototypeRegistryService().cachePrototype(aTypeName, aPrototype);
  }
  public boolean isPrototypeCached(String aTypeName) {
    return getPrototypeRegistryService().isPrototypeCached(aTypeName);
  }    
  public Asset getPrototype(String aTypeName) {
    return getPrototype(aTypeName, null);
  }
  public Asset getPrototype(String aTypeName, Class anAssetClass) {
    return getPrototypeRegistryService().getPrototype(aTypeName, anAssetClass);
  }
  public void fillProperties(Asset anAsset) {
    getPrototypeRegistryService().fillProperties(anAsset);
  }
  public void addPropertyProvider(PropertyProvider prov) {
    getPrototypeRegistryService().addPropertyProvider(prov);
  }
  public void addLatePropertyProvider(LatePropertyProvider lpp) {
    getPrototypeRegistryService().addLatePropertyProvider(lpp);
  }
  public PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pgclass, long t) {
    return getPrototypeRegistryService().lateFillPropertyGroup(anAsset, pgclass, t);
  }


  // 
  //Domain Service backwards compatability
  //

  /** 
   * Answer with a reference to the Factory
   * It is inteded that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  public ClusterObjectFactory getClusterObjectFactory()  {
    return getDomainService().getClusterObjectFactory();
  }
  /** expose the LDM factory instance to consumers.
   *    @return LdmFactory The fatory object to use in constructing LDM Objects
   **/
  public RootFactory getFactory(){
    return getDomainService().getFactory();
  }
  /** @deprecated use getFactory() **/
  public RootFactory getLdmFactory() {
    return getFactory();
  }
  /** create a domain-specific factory **/
  public Factory getFactory(String domainname) {
    return getDomainService().getFactory(domainname);
  }
  /** Expose the Registry to consumers. 
   **/
  // try to get rid of this...
  //public Registry getRegistry() {
  //  return myRegistry;
  //}


  /**
   * Temporary support for <code>MessageTransportServiceProvider</code>.  
   */
  MessageTransportService getMessageTransportServer() {
    return messenger;
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

  // hook into Agent
  public ClusterIdentifier getAgentIdentifier() {
    return this.getClusterIdentifier();
  }
  // additional ClusterContext implementation

  /** Answer with your represenation as a ClusterIdentifier instance. **/
  public ClusterIdentifier getClusterIdentifier() { 
    return myClusterIdentity_;
  }

  public LDMServesPlugIn getLDM() {
    return this;
  }

  /**
   * @return a new filled in <code>MetricsSnapshot</code>
   * @deprecated use #getMetricsSnapshot(MetricsSnapshot ms) and reuse the object
   */
  public MetricsSnapshot getMetricsSnapshot() {
    return getMetricsSnapshot(new MetricsSnapshot(), true);
  }

  public MetricsSnapshot getMetricsSnapshot(MetricsSnapshot ms, boolean resetMsgStats) {
    if (ms == null)
      ms = new MetricsSnapshot();
    ms.clusterName = getClusterIdentifier().cleanToString();
    ms.nodeName = getBindingSite().getName();
    ms.time = System.currentTimeMillis();

    // message transport stuff
    MessageWatcher mw = getMessageWatcher();
    ms.directivesIn = mw.directivesIn;
    ms.directivesOut = mw.directivesOut;
    ms.notificationsIn = mw.notificationsIn;
    ms.notificationsOut = mw.notificationsOut;

    // Message Statistics stuff
    MessageStatistics.Statistics mstats = getMessageStatistics(resetMsgStats);
    if (mstats != null) {
      ms.averageMessageQueueLength = mstats.averageMessageQueueLength;
      ms.totalMessageBytes = mstats.totalMessageBytes;
      ms.totalMessageCount = mstats.totalMessageCount;
    }
    
    // logplan stuff
    if (myLogPlan != null) {
      ms.assets = myLogPlan.getAssetCount();
      ms.planelements = myLogPlan.getPlanElementCount();
      ms.tasks = myLogPlan.getTaskCount();
      ms.workflows = myLogPlan.getWorkflowCount();
    }

    // cluster metrics
    ms.pluginCount = pluginManager.size();
    //no longer works from cluster as the sharedpluginmanager
    //is now a service
    //ms.thinPluginCount = getSharedPlugInManager().size();
    //no longer works from cluster as these are part of 
    //the prototyperegistryservice
    //ms.prototypeProviderCount = prototypeProviders.size();
    //ms.propertyProviderCount = propertyProviders.size();
    //ms.cachedPrototypeCount = getRegistry().size();

    // vm stuff
    ms.idleTime = getIdleTime();

    // do a gc to ensure get accurate results.
    // Note this means that if you call this method frequently, you'll hurt
    // your performance with all these garbage collects
    Runtime.getRuntime().gc();
    
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
   * deactivated by -Dorg.cougaar.core.cluster.heartbeat=false
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
  // COUGAAR Scenario Time management and support for PlugIns
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
  * This method sets the COUGAAR scenario time to a specific time
  * in the future.  
  * @param time milliseconds in java time.
  * @param running should the clock continue to run after setting the time?
  **/
  public void setTime(long time, boolean running) {
    sendAdvanceClockMessage(time, true, 0.0, running, _xTimer.DEFAULT_CHANGE_DELAY);
  }
	
 /**
  * This method sets the COUGAAR scenario time to a specific rate.
  * @param newRate the new rate. Execution time advance at the new rate after a brief delay
  **/
  public void setTimeRate(double newRate) {
    sendAdvanceClockMessage(0L, false, newRate, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }
	
  /**
   * This method advances the COUGAAR scenario time a period of time
   * in the future, leaving the clock stopped.
   * equivalent to advanceTime(timePeriod, false);
   **/
  public void advanceTime(long timePeriod){
    sendAdvanceClockMessage(timePeriod, false, 0.0, false, _xTimer.DEFAULT_CHANGE_DELAY);
  }

  /**
   * This method advances the COUGAAR scenario time a period of time
   * in the future.
   * @param timePeriod Milliseconds to advance the scenario clock.
   * @param running should the clock continue to run after setting.
   **/
  public void advanceTime(long timePeriod, boolean running){
    sendAdvanceClockMessage(timePeriod, false, 0.0, running, _xTimer.DEFAULT_CHANGE_DELAY);
  }

  /**
   * This method advances the COUGAAR scenario time a period of time
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
   * This method gets the current COUGAAR scenario time. 
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
