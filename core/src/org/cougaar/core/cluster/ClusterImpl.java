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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.StateModelException;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentBindingSite;

import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;

import org.cougaar.core.society.ComponentMessage;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageStatistics;

// cluster context registration
import org.cougaar.core.cluster.ClusterContext;

// blackboard support
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.core.blackboard.BlackboardServiceProvider;

// message-transport support
import org.cougaar.core.mts.MessageStatisticsService;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.mts.MessageWatcherService;

// LDM service
import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.plugin.LDMServiceProvider;

// prototype and property providers
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;

// domain and factory support
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Domain;
import org.cougaar.domain.planning.ldm.DomainManager;
import org.cougaar.domain.planning.ldm.DomainService;
import org.cougaar.domain.planning.ldm.DomainServiceImpl;
import org.cougaar.domain.planning.ldm.DomainServiceProvider;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;

// types for factory support
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;

// prototype registry service
import org.cougaar.domain.planning.ldm.PrototypeRegistry;
import org.cougaar.domain.planning.ldm.PrototypeRegistryService;
import org.cougaar.domain.planning.ldm.PrototypeRegistryServiceProvider;

// Object factories
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactoryImpl;

// Scenario time support
import org.cougaar.core.cluster.AdvanceClockMessage;
import org.cougaar.core.cluster.Alarm;

// Persistence
import org.cougaar.core.cluster.persist.DatabasePersistence;
import org.cougaar.core.cluster.persist.Persistence;

import org.cougaar.util.PropertyParser;

/**
 * Implementation of Agent which creates a PlugInManager and Blackboard and 
 * provides basic services to Agent Components.
 * <p>
 * <pre>
 * @property org.cougaar.core.cluster.heartbeat
 *   If enabled, a low-priority thread runs and prints
 *   a '.' every few seconds when nothing else much is going on.
 *   This is a one-per-vm function.  Default <em>true</em>.
 * @property org.cougaar.core.cluster.idleInterval 
 * How long between idle detection and heartbeat cycles (prints '.');
 * @property org.cougaar.core.cluster.idle.verbose
 *   If <em>true</em>, will print elapsed time (seconds) since
 *   cluster start every idle.interval millis.
 * @property org.cougaar.core.cluster.idle.verbose.interval=60000
 *   The number of milliseconds between verbose idle reports.
 * @property org.cougaar.core.cluster.showTraffic
 *   If <em>true</em>, shows '+' and '-' on message sends and receives.  if
 *   <em>false</em>, also turns off reports of heartbeat ('.') and other status chars.
 * </pre>
 */
public class ClusterImpl 
  extends Agent
  implements Cluster, LDMServesPlugIn, ClusterContext, MessageTransportClient, MessageStatistics, StateObject
{
  // services, in order of "load()"
  private MessageTransportService messenger;
  private MessageStatisticsService statisticsService;
  private MessageWatcherService watcherService;

  private UIDServiceProvider myUIDServiceProvider;
  private UIDService myUIDService;

  private PrototypeRegistryService myPrototypeRegistryService;
  private PrototypeRegistryServiceProvider myPrototypeRegistryServiceProvider;

  private DomainServiceProvider myDomainServiceProvider;
  private DomainService myDomainService;

  private AlarmServiceProvider myAlarmServiceProvider;

  private DemoControlServiceProvider myDemoControlServiceProvider;

  private SchedulerServiceProvider mySchedulerServiceProvider;

  private LDMServiceProvider myLDMServiceProvider;

  private BlackboardForAgent myBlackboardService;

  // properties
  private static boolean isHeartbeatOn = true;
  private static int idleInterval = 5*1000;
  private static boolean idleVerbose = false; // don't be verbose
  private static long idleVerboseInterval = 60*1000L; // 1 minute
  private static long maxIdleInterval;
  private static boolean usePlugInLoader = false;
  private static boolean showTraffic = true;

  static {
    isHeartbeatOn=PropertyParser.getBoolean("org.cougaar.core.cluster.heartbeat", true);
    usePlugInLoader=PropertyParser.getBoolean("org.cougaar.core.cluster.pluginloader", false);
    idleInterval=PropertyParser.getInt("org.cougaar.core.cluster.idleInterval", 5000);
    maxIdleInterval = (idleInterval+(idleInterval/10));
    showTraffic=PropertyParser.getBoolean("org.cougaar.core.cluster.showTraffic", true);
    idleVerbose = PropertyParser.getBoolean("org.cougaar.core.cluster.idle.verbose", true);
    idleVerboseInterval = PropertyParser.getInt("org.cougaar.core.cluster.idle.verbose.interval", 60000);
  }


  private AgentBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentBindingSite) {
      bindingSite = (AgentBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
  }

  protected final AgentBindingSite getBindingSite() {
    return bindingSite;
  }

  //
  // services
  //

  // UID Service
  /** @deprecated use getUIDService() **/
  public final UIDServer getUIDServer() {
    return myUIDService;
  }
  //public for backwards compatability for now
  public final UIDService getUIDService() {
    return myUIDService;
  }

  //Prototype Service
  protected final PrototypeRegistryService getPrototypeRegistryService() {
    return myPrototypeRegistryService;
  }

  //Domain/Factory Service
  protected final DomainService getDomainService() {
    return myDomainService;
  }

  // end services

  /**
   * myClusterIdentity_ is a private representation of this instance's
   * ClusterIdentifier.
   **/
  private ClusterIdentifier myClusterIdentity_;
    
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

  /**
   * Expects the parameter to specify the ClusterIdentifier,
   * either through a single String or the first element of
   * a List.
   */
  public void setParameter(Object o) {
    String sID = null;
    if (o instanceof String) {
      sID = (String)o;
    } else if (o instanceof List) {
      List l = (List)o;
      if (l.size() > 0) {
        Object o1 = l.get(0);
        if (o1 instanceof String) {
          sID = (String)o1;
        }
      }
    }
    if (sID != null) {
      setClusterIdentifier(new ClusterIdentifier(sID));
    }
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
      throw new StateModelException(
          "Container ("+getBindingSite()+") does not implement AgentBindingSite");
    }
    ServiceBroker sb = getServiceBroker();

    // get the Messenger instance from ClusterManagement
    messenger = (MessageTransportService) 
      sb.getService(this, MessageTransportService.class, null);
    messenger.registerClient(this);

    statisticsService = (MessageStatisticsService) 
      sb.getService(
          this, MessageStatisticsService.class, null);

    watcherService = (MessageWatcherService) 
      sb.getService(
          this, MessageWatcherService.class, null);


    // add ourselves to our VM's cluster table
    ClusterContextTable.addContext(getClusterIdentifier(), this);

    // set up the UIDServer and UIDService
    UIDServiceImpl theUIDServer = new UIDServiceImpl(this);
    myUIDServiceProvider = new UIDServiceProvider(theUIDServer);
    sb.addService(UIDService.class, myUIDServiceProvider);

    myUIDService = (UIDService) sb.getService(this, UIDService.class, null);
    
    // set up the PrototypeRegistry and the PrototypeRegistryService
    PrototypeRegistry thePrototypeRegistry = new PrototypeRegistry();
    myPrototypeRegistryServiceProvider = 
      new PrototypeRegistryServiceProvider(thePrototypeRegistry);
    sb.addService(
        PrototypeRegistryService.class, 
        myPrototypeRegistryServiceProvider);

    //for backwards compatability
    myPrototypeRegistryService = (PrototypeRegistryService) 
      sb.getService(
          this, PrototypeRegistryService.class, null);

    //set up the DomainServiceImpl and the DomainService
    // DomainServiceImpl needs the PrototypeRegistryService
    //for now its in the form of this as LDMServesPlugin - should be changed!!!
    DomainServiceImpl myDSI = new DomainServiceImpl(this);
    myDomainServiceProvider = new DomainServiceProvider(myDSI);
    sb.addService(DomainService.class, myDomainServiceProvider);

    //for backwards compatability
    myDomainService = (DomainService) 
      sb.getService(
          this, DomainService.class, null);

    // add alarm service
    myAlarmServiceProvider = new AlarmServiceProvider(this);
    sb.addService(AlarmService.class, myAlarmServiceProvider);

    // hack service for demo control
    myDemoControlServiceProvider = new DemoControlServiceProvider(this);
    sb.addService(DemoControlService.class, myDemoControlServiceProvider);

    // scheduler for new plugins
    mySchedulerServiceProvider = new SchedulerServiceProvider(this);
    sb.addService(SchedulerService.class, mySchedulerServiceProvider);

    // placeholder for LDM Services should go away and be replaced by the
    // above domainservice and prototyperegistry service
    myLDMServiceProvider = new LDMServiceProvider(this);
    sb.addService(LDMService.class, myLDMServiceProvider);

    // force set up all the factories
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

    if (loadState instanceof AgentState) {
      // use the existing state
      AgentState agentState = (AgentState) loadState;
      this.loadState = null;
      // load the child Components (Plugins, etc)
      int n = ((agentState.children != null) ? agentState.children.length : 0);
      for (int i = 0; i < n; i++) {
        add(agentState.children[i]);
      }
    } else {
      // blackboard *MUST* be loaded before pluginmanager (and plugins)
      add(new ComponentDescription(getClusterIdentifier()+"Blackboard",
                                   "Node.AgentManager.Agent.Blackboard",
                                   "org.cougaar.core.blackboard.StandardBlackboard",
                                   null,
                                   null,
                                   null,
                                   null,
                                   null));

      // start up the pluginManager component - should really itself be loaded
      // as an agent subcomponent.
      //create a component description for pluginmanager instead of an instance
      String pimname = new String(getClusterIdentifier()+"PluginManager");
      ComponentDescription pimdesc = 
        new ComponentDescription(
            pimname,
            "Node.AgentManager.Agent.PluginManager",
            "org.cougaar.core.plugin.PluginManager",
            null,  //codebase
            null,  //parameters
            null,  //certificate
            null,  //lease
            null); //policy

      add(pimdesc);
    }

    // get blackboard service
    myBlackboardService = (BlackboardForAgent) 
      sb.getService(this, BlackboardForAgent.class, null);
    if (myBlackboardService == null) {
      throw new RuntimeException("Couldn't get BlackboardForAgent!");
    }

    //System.err.println("Cluster.load() completed");
  }

  /** Called object should start any threads it requires.
   *  Called object should transition to the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void start() throws StateModelException {

    if (isHeartbeatOn) {
      startHeartbeat();
    }

    super.start();

    // start the message queue
    startQueueHandler();

    // register with node - temporary hack.
    getBindingSite().registerAgent(this);

  }


  public void suspend() {
    super.suspend();

    // suspend all children
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.suspend();
    }

    // suspend the alarms

    System.out.println("suspend scheduler service");
    mySchedulerServiceProvider.suspend(); // suspend the plugin scheduling

    System.out.println("unregisterClient");
    messenger.unregisterClient(ClusterImpl.this);

    getQueueHandler().halt();
  }

  public void resume() {
    super.resume();

    // re-register for MessageTransport

    mySchedulerServiceProvider.resume(); // suspend the plugin scheduling

    // resume the alarms 

    // resume all children
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.resume();
    }
  }


  public void stop() {
    super.stop();

    // should be okay...

    // stop all children
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.stop();
    }
  }

  public void halt() {
    // this seems reasonable:
    suspend();
    stop();
  }

  public void unload() {
    super.unload();

    // unload in reverse order of "load()"

    ServiceBroker sb = getServiceBroker();

    // release child services
    sb.releaseService(this, BlackboardForAgent.class, myBlackboardService);

    // unload children
    for (Iterator childBinders = binderIterator();
         childBinders.hasNext();
         ) {
      Binder b = (Binder)childBinders.next();
      b.unload();
    }
    boundComponents.clear();

    //
    // release context-based services
    //

    sb.revokeService(LDMService.class, myLDMServiceProvider);

    sb.revokeService(SchedulerService.class, mySchedulerServiceProvider);

    sb.revokeService(DemoControlService.class, myDemoControlServiceProvider);

    sb.revokeService(AlarmService.class, myAlarmServiceProvider);

    sb.releaseService(this, DomainService.class, myDomainService);
    sb.revokeService(DomainService.class, myDomainServiceProvider);

    sb.releaseService(this, PrototypeRegistryService.class, 
                      myPrototypeRegistryService);
    sb.revokeService(PrototypeRegistryService.class, 
                     myPrototypeRegistryServiceProvider);

    sb.releaseService(this, UIDService.class, myUIDService);
    sb.revokeService(UIDService.class, myUIDServiceProvider);

    // remove ourselves from the VM-local context
    ClusterContextTable.removeContext(getClusterIdentifier());

    //
    // release remaining services
    //

    sb.releaseService(this, MessageWatcherService.class, watcherService);
    sb.releaseService(this, MessageStatisticsService.class, statisticsService);
    sb.releaseService(this, MessageTransportService.class, messenger);
  }

  /**
   * Get the state of this cluster, which should be suspended.
   *
   * Need to fix ContainerSupport for locking and hide
   * "boundComponents" access.
   */
  public Object getState() {
    AgentState result = new AgentState();
    // get the child components
    synchronized (boundComponents) {
      int n = boundComponents.size();
      result.children = new StateTuple[n];
      for (int i = 0; i < n; i++) {
        org.cougaar.core.component.BoundComponent bc = 
          (org.cougaar.core.component.BoundComponent)
          boundComponents.get(i);
        Object comp = bc.getComponent();
        if (comp instanceof ComponentDescription) {
          ComponentDescription cd = (ComponentDescription)comp;
          Binder b = bc.getBinder();
          Object state = b.getState();
          result.children[i] = new StateTuple(cd, state);
        } else {
          // error?
        }
      }
    }

    // Do this here because we might not get another opportunity
    System.out.println("flushMessages");
    messenger.flushMessages();
    
    return result;
  }

  private Object loadState;

  public void setState(Object loadState) {
    this.loadState = loadState;
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
            super.add(desc);     
            break;
          case ComponentMessage.REMOVE:  
            super.remove(desc);  
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
        // internal message queue
        getQueueHandler().addMessage((ClusterMessage)message);
      } else {
        System.err.println("\n"+this+": Received unhandled Message ("+message.getClass()+"): "+message);
      }
    } catch( Exception e) {
      System.err.println("Unhandled Exception: "+e);
      e.printStackTrace();
    }
  }

  /** Receiver for in-band messages (queued by QueueHandler)
   */
  void receiveQueuedMessages(List messages) {
    myBlackboardService.receiveMessages(messages);
  }

  //
  // MessageStatistics
  //

  public MessageStatistics.Statistics getMessageStatistics(boolean reset) {
      if (statisticsService != null) {
	  return statisticsService.getMessageStatistics(reset);
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
      System.err.println(
          "QueueHandler in " + getClusterIdentifier() + " asked to restart.");
    }
  }

  private QueueHandler getQueueHandler() {
    synchronized (this) {
      if (queueHandler == null) {
        queueHandler = new QueueHandler(this);
      }
      return queueHandler;
    }
  }

  private static class QueueHandler extends Thread {
    private ClusterImpl cluster;
    private List queue = new ArrayList();
    private List msgs = new ArrayList();
    private boolean running = true;
    public QueueHandler(ClusterImpl cluster) {
      super(cluster.getClusterIdentifier().getAddress() + "/RQ");
      this.cluster = cluster;
    }
    public void halt() {
      synchronized (queue) {
        running = false;
        queue.notify();
      }
      try {
        // wait for this thread to stop
        join(); 
      } catch (InterruptedException ie) {
      }
      cluster = null;
    }
    public void run() {
      ClusterMessage m;
      int size;
      while (running) {
        synchronized (queue) {
          while (running && queue.isEmpty()) {
            try {
              queue.wait();
            }
            catch (InterruptedException ie) {
            }
          }
          msgs.addAll(queue);
          queue.clear();
        }
        if (msgs.size() > 0) {
          cluster.receiveQueuedMessages(msgs);
        }
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
   *
   * Should be moved into Node as a Node-level Service.
   **/
  private static class Heartbeat implements Runnable {
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

  private static void startHeartbeat() {
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

  private static void showProgress(String p) {
    if (showTraffic) {
      // Too many threads in a multi-cluster node are printing progress 
      // at the same time and we don't really care about the newlines
      // so we'll drop the synchronized and live with the consequences.
      System.out.print(p);
    }
  }

  /** @deprecated Use BlackboardService.getPersistence().getDatabaseConnection() **/
  public java.sql.Connection getDatabaseConnection(Object locker) { return null; }

  /** @deprecated Use BlackboardService.getPersistence().releaseDatabaseConnection() **/
  public void releaseDatabaseConnection(Object locker) {}

  private static class AgentState implements java.io.Serializable {
    StateTuple[] children;
  }
}
