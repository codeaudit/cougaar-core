/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.agent;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmServiceProvider;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.agent.service.democontrol.DemoControlServiceProvider;
import org.cougaar.core.blackboard.Blackboard; // inlined
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mobility.MobileAgentService;
import org.cougaar.core.mts.Attributes;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageHandler;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.ComponentMessage;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.node.service.RealTimeService;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceObject;
import org.cougaar.core.persist.PersistenceServiceForAgent;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.persist.PersistenceServiceComponent;
import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.EventService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.identity.AgentIdentityClient;
import org.cougaar.core.service.identity.AgentIdentityService;
import org.cougaar.core.service.identity.CrlReason;
import org.cougaar.core.service.identity.TransferableIdentity;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.Callback;
import org.cougaar.core.service.wp.Response;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.StateModelException;
import org.cougaar.util.log.Logging;

/**
 * Implementation of Agent which creates a PluginManager and Blackboard and 
 * provides basic services to Agent Components.
 * <p>
 * @property org.cougaar.core.agent.showTraffic
 *   If <em>true</em>, shows '+' and '-' on message sends and receives.
 *
 * @property org.cougaar.core.agent quiet
 *   Makes standard output as quiet as possible.
 *
 * @property org.cougaar.core.agent.removalCheckMillis
 *   If set, this indicates the number of milliseconds after
 *   agent "unload()" for a GC check of the agent.  This is 
 *   primarily for agent mobility memory-leak testing.
 *
 * @property org.cougaar.core.load.community
 *   If enabled, the agent will load the CommunityService
 *   component.  See bug 2522.  Default <em>true</em>
 *
 * @property org.cougaar.core.load.planning
 *   If enabled, the agent will load the planning-specific
 *   NodeTrustComponent or AssetInitializerService.  See bug 2522.
 *   Default <em>true</em>
 *
 * @property org.cougaar.core.load.servlet
 *   If enabled, the agent will load the ServletService
 *   component.  See bug 2522.  Default <em>true</em>
 */
public class SimpleAgent 
extends Agent
implements AgentIdentityClient
{
  // this node's address
  private MessageAddress localNode;

  // incarnation for this agent, which is incremented every time
  // this agent restarts but not when the agent moves.
  private long incarnation;

  // move identity of this agent, which is incremented every time this
  // agent moves.
  private long moveId;

  private PersistenceServiceForAgent persistenceService;

  private PersistenceIdentity persistenceIdentity =
    new PersistenceIdentity(getClass().getName());

  private PersistenceClient persistenceClient =
    new PersistenceClient() {
      public PersistenceIdentity getPersistenceIdentity() {
        return persistenceIdentity;
      }
      public List getPersistenceData() {
        return SimpleAgent.this.getPersistenceData();
      }
    };
        
  // state for this agent if it is arriving from a move.
  // The PersistenceObject if doing agent transfer
  private PersistenceObject persistenceObject;

  // state for this agent as retrieved from the PersistenceService
  // this is set and used within "load()"
  private PersistenceData persistenceData;

  // state from "suspend()", used within "getState()"
  private List unsentMessages;
  private org.cougaar.core.mts.AgentState mtsState;

  // mobility destination node address
  private MessageAddress moveTargetNode;

  // mobility transferable identity
  private TransferableIdentity mobileIdentity;

  // dummy mobility identity, in case the agent-id-service
  // passes back a null transfer identity
  //
  // the AgentIdentityService API should be fixed to guarantee
  // a non-null transferable identity
  private static final TransferableIdentity NULL_MOBILE_IDENTITY =
    new TransferableIdentity() {
      private Object readResolve() { return NULL_MOBILE_IDENTITY; }
    };

  // services, in order of "load()"

  private String localHost;
  private EventService eventService;
  private LoggingService log = LoggingService.NULL;

  private AgentIdentityService myAgentIdService;

  private WhitePagesService whitePagesService;

  private MessageTransportService messenger;
  /** stub for keeping the MTS client at a distance **/
  private MessageTransportClient mtsClientAdapter;

  private MobileAgentService myMobileAgentService;

  private NaturalTimeService xTimer;
  private RealTimeService rTimer;
  private AlarmServiceProvider myAlarmServiceProvider;
  private DemoControlServiceProvider myDemoControlServiceProvider;
  private ServiceProvider myMessageSwitchSP;

  protected BlackboardForAgent myBlackboardService;

  // map of agent name to most recently observed incarnation, used
  // to detect the restart of remote agents, which requires a
  // resync beteen this agent and the restarted agent.
  private boolean needsRestart = true;
  private final Map incarnationMap = new HashMap();

  // properties
  private static final boolean VERBOSE_RESTART = true;//false;
  private static final long RESTART_CHECK_INTERVAL = 43000L;
  private static final boolean showTraffic;
  private static final boolean isQuiet;
  private static final boolean isCommunityEnabled;
  private static final boolean isPlanningEnabled;
  private static final boolean isServletEnabled;

  static {
    showTraffic=PropertyParser.getBoolean("org.cougaar.core.agent.showTraffic", true);
    isQuiet=PropertyParser.getBoolean("org.cougaar.core.agent.quiet", false);
    isCommunityEnabled=PropertyParser.getBoolean("org.cougaar.core.load.community", true);
    isPlanningEnabled=PropertyParser.getBoolean("org.cougaar.core.load.planning", true);
    isServletEnabled=PropertyParser.getBoolean("org.cougaar.core.load.servlet", true);
  }

  private Timer restartTimer;

  /**
   * myMessageAddress_ is a private representation of this instance's
   * MessageAddress.
   **/
  private MessageAddress myMessageAddress_;

  /** Alias for getMessageAddress, required by Agent superclass **/
  public MessageAddress getAgentIdentifier() {
    return this.getMessageAddress();
  }

  /**
   * Answer with a String representation of yourself.
   * @return String String representation of this instance.
   */
  public String toString()
  {
    String body = "anonymous";
    MessageAddress cid = getMessageAddress();
    if (cid != null)
      body = cid.getAddress();
    return "<Agent " + body + ">";
  }

  /**
   * Expects the parameter to specify the MessageAddress,
   * either through a single String or the first element of
   * a List.
   */
  public void setParameter(Object o) {
    MessageAddress cid = null;
    if (o instanceof MessageAddress) {
      cid = (MessageAddress) o;
    } else if (o instanceof String) {
      cid = MessageAddress.getMessageAddress((String) o);
    } else if (o instanceof List) {
      List l = (List)o;
      if (l.size() > 0) {
        Object o1 = l.get(0);
        if (o1 instanceof MessageAddress) {
          cid = (MessageAddress) o1;
        } else if (o1 instanceof String) {
          cid = MessageAddress.getMessageAddress((String) o1);
        }
      }
    }
    this.myMessageAddress_ = cid;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    // always use "findExternalComponentDescriptions()"
    return null;
  }

  /**
   * Get the components from the ComponentInitializerService or the
   * state. If persistenceData is non-null, our components are defined
   * within that data. Otherwise, use the ComponentInitializerService.
   **/
  protected ComponentDescriptions findExternalComponentDescriptions() {
    if (persistenceData != null) {
      // get descriptions from mobile state
      ComponentDescriptions descs = persistenceData.descs;
      // fix tuples where the desc.getParameter is an 
      // "InternalAdapter", since these are pointers into *this*
      // agent.  This is required for agent mobility to work.
      List fixme = 
        descs.selectComponentDescriptions(
            ComponentDescription.PRIORITY_COMPONENT);
      for (int i=0, n=fixme.size(); i<n; i++) {
        Object oi = fixme.get(i);
        ComponentDescription desc;
        if (oi instanceof StateTuple) {
          StateTuple st = (StateTuple) oi;
          desc = st.getComponentDescription();
        } else if (oi instanceof ComponentDescription) {
          desc = (ComponentDescription) oi;
        } else {
          desc = null;
        }
        if (desc != null) {
          Object p = desc.getParameter();
          if (p instanceof InternalAdapter) {
            if (log.isInfoEnabled()) {
              log.info("Setting agent back-pointer in "+desc);
            }
            ((InternalAdapter) p).setAgent(this);
          }
        }
      }
      return descs;
    }

    String cname = getIdentifier();
    ServiceBroker sb = getServiceBroker();
    ComponentInitializerService cis = (ComponentInitializerService) 
      sb.getService(this, ComponentInitializerService.class, null);
    try {
      String cp = specifyContainmentPoint();
      ComponentDescription[] cds = new ComponentDescription[0];
      try {
	// Get components _below_ given point
	// That is, we want all items below the cp. 
        cds = cis.getComponentDescriptions(cname, cp);
      } catch (ComponentInitializerService.InitializerException cise) {
        if (log.isWarnEnabled()) {
          log.warn("Cannot find Agent configuration for "+cname, cise);
        }
      }

      //
      // FIXME by *luck* our descriptions are in this order:
      //
      //   - ".ini" high-priority components
      //   - hard-coded component-priority components
      //   - ".ini" component-priority components
      //   - ".ini" low-priority components
      //   - hard-coded low-priority components
      //
      // Note that all hard-coded components at all priority levels 
      // are either all before or after all same-priority ".ini" 
      // components.
      //
      // Here we take advantage of that property and merge all
      // hard-coded and ".ini" components into one list.
      //
      List l = new ArrayList(cds.length + 14);

      // advertise our agent id
      l.add(new ComponentDescription(
            getMessageAddress()+"Addr",
            Agent.INSERTION_POINT+".Addr",
            "org.cougaar.core.agent.service.id.AgentIdentificationServiceComponent",
            null,
            getMessageAddress(),
            null,
            null,
            null,
            ComponentDescription.PRIORITY_HIGH));

      // event service (wrapper for logging)
      l.add(new ComponentDescription(
            getMessageAddress()+"Event",
            Agent.INSERTION_POINT+".Event",
            "org.cougaar.core.agent.service.event.EventServiceComponent",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_HIGH));


      // add the containment service
      l.add(new ComponentDescription(
            getMessageAddress()+"Contain",
            Agent.INSERTION_POINT + ".Contain",
            "org.cougaar.core.agent.service.containment.AgentContainmentServiceComponent",
            null,
            (new AgentContainmentAdapter(this)), // access into *this* container
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

      // set up the UIDServer and UIDService
      l.add(new ComponentDescription(
            getMessageAddress()+"UID",
            Agent.INSERTION_POINT + ".UID",
            "org.cougaar.core.agent.service.uid.UIDServiceComponent",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

      // thread service
      l.add(new ComponentDescription(
            getMessageAddress()+"Threads",
            Agent.INSERTION_POINT + ".Threads",
            "org.cougaar.core.thread.ThreadServiceProvider",
            null,
            ("name=Agent_"+getMessageAddress()),
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

      // scheduler for new plugins
      l.add(new ComponentDescription(
            getMessageAddress()+"Sched",
            Agent.INSERTION_POINT + ".Sched",
            "org.cougaar.core.agent.service.scheduler.SchedulerServiceComponent",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

      if (isPlanningEnabled) {
        // set up the PrototypeRegistry and the PrototypeRegistryService
        // this needs to happen before we start accepting messages.
        l.add(new ComponentDescription(
              getMessageAddress()+"ProtoReg",
              Agent.INSERTION_POINT + ".ProtoReg",
              "org.cougaar.planning.ldm.PrototypeRegistryServiceComponent",
              null,
              null,
              null,
              null,
              null,
              ComponentDescription.PRIORITY_COMPONENT));

        // ldm service
        l.add(new ComponentDescription(
              getMessageAddress()+"LDM",
              Agent.INSERTION_POINT + ".LDM",
              "org.cougaar.planning.ldm.LDMServiceComponent",
              null,
              null,
              null,
              null,
              null,
              ComponentDescription.PRIORITY_COMPONENT));
      }

      if (isServletEnabled) {
        // start up the Agent-level ServletService component
        l.add(new ComponentDescription(
              getMessageAddress()+"ServletService",
              Agent.INSERTION_POINT + ".AgentServletService",
              "org.cougaar.lib.web.service.LeafServletServiceComponent",
              null,  //codebase
              getMessageAddress().getAddress(),
              null,  //certificate
              null,  //lease
              null,  //policy
              ComponentDescription.PRIORITY_COMPONENT));
      }

      // Domains *MUST* be loaded before the blackboard
      l.add(new ComponentDescription(
            getMessageAddress()+"DomainManager",
            org.cougaar.core.domain.DomainManager.INSERTION_POINT,
            "org.cougaar.core.domain.DomainManager",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));

      if (isCommunityEnabled) {
        // CommunityService *MUST* be loaded before the blackboard
        l.add(new ComponentDescription(
              getMessageAddress()+"CommunityService",
              Agent.INSERTION_POINT + ".Component",
              "org.cougaar.community.CommunityServiceComponent",
              null,
              null,
              null,
              null,
              null,
              ComponentDescription.PRIORITY_COMPONENT));
      }

      // blackboard *MUST* be loaded before pluginmanager (and plugins)
      l.add(new ComponentDescription(
            getMessageAddress()+"Blackboard",
            Blackboard.INSERTION_POINT,
            "org.cougaar.core.blackboard.StandardBlackboard",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_COMPONENT));
      // add in ".ini" descriptions
      for (int i = 0; i < cds.length; i++) {
        l.add(cds[i]);
      }
      // final step is to load the plugin manager which will make 
      // the plugins go.
      l.add(new ComponentDescription(
            getMessageAddress()+"PluginManager",
            org.cougaar.core.plugin.PluginManager.INSERTION_POINT,
            "org.cougaar.core.plugin.PluginManager",
            null,
            null,
            null,
            null,
            null,
            ComponentDescription.PRIORITY_LOW));

      return new ComponentDescriptions(l);
    } catch (Exception e) {
      log.error("Unable to add "+cname+"'s child Components", e);
      return null;
    } finally {
      sb.releaseService(this, ComponentInitializerService.class, cis);
    }
  }

  protected PersistenceClient getPersistenceClient() {
    return persistenceClient;
  }

  protected List getRehydrationList(
      PersistenceServiceForAgent persistenceService) {
    RehydrationData rd = persistenceService.getRehydrationData();
    if (rd != null) {
      return rd.getObjects();
    }
    return null;
  }

  public void load() {

    // get our log
    LoggingService newLog = (LoggingService) 
      getServiceBroker().getService(
          this, LoggingService.class, null);
    if (newLog != null) {
      String prefix = getMessageAddress()+": ";
      log = LoggingServiceWithPrefix.add(newLog, prefix);
    }

    if (log.isInfoEnabled()) {
      log.info("Loading");
    }

    ComponentDescription persistenceComponentDescription =
      new ComponentDescription(
          getMessageAddress() + "Persist",
          Agent.INSERTION_POINT + ".Persist",
          "org.cougaar.core.persist.PersistenceServiceComponent",
          null,
          getMessageAddress(),
          null,
          null,
          null,
          ComponentDescription.PRIORITY_INTERNAL);
    super.add(persistenceComponentDescription);

    // add our address to our VM's cluster table
    if (log.isDebugEnabled()) {
      log.debug("Adding to the cluster context table");
    }
    ClusterContextTable.addContext(getMessageAddress());

    persistenceService = (PersistenceServiceForAgent)
      getChildServiceBroker().getService(
          getPersistenceClient(), PersistenceServiceForAgent.class, null);
    persistenceService.rehydrate(persistenceObject);
    List rehydrationList = getRehydrationList(persistenceService);
    if (rehydrationList != null && rehydrationList.size() > 0) {
      persistenceData = (PersistenceData) rehydrationList.get(0);
      // validate the state
      // verify state's agent id
      if (!(getMessageAddress().equals(persistenceData.agentId))) {
        if (log.isErrorEnabled()) {
          log.error(
              "Load state contains incorrect agent address " +
              persistenceData.agentId);
        }
        // continue anyways
      }
    } else {
      persistenceData = null;
    }

    // do the standard thing.
    super.load();

    // get event service
    eventService = (EventService)
      getServiceBroker().getService(
          this, EventService.class, null);

    if (log.isInfoEnabled()) {
      log.info("Loaded");
    }
  }

  protected void loadHighPriorityComponents() {
    ServiceBroker sb = getServiceBroker();

    // get the local node id
    NodeIdentificationService nodeIdService = 
      (NodeIdentificationService)
      sb.getService(this, NodeIdentificationService.class, null);
    if (nodeIdService == null) {
      throw new RuntimeException("Unable to get node-id service");
    }
    localNode = nodeIdService.getMessageAddress();
    if (localNode == null) {
      throw new RuntimeException("Local node address is null?");
    }
    sb.releaseService(
        this, NodeIdentificationService.class, nodeIdService);

    super.loadHighPriorityComponents();
  }

  protected void loadInternalPriorityComponents() {
    ServiceBroker sb = getServiceBroker();
    ServiceBroker csb = getChildServiceBroker();

    // lookup localhost, for WP and event use
    try {
      InetAddress localAddr = InetAddress.getLocalHost();
      localHost = localAddr.getHostName();
    } catch (Exception e) {
      localHost = "?";
    }

    // acquire our identity
    TransferableIdentity priorIdentity = 
      ((persistenceData != null) ?
       persistenceData.identity :
       null);
    if (priorIdentity == NULL_MOBILE_IDENTITY) {
      priorIdentity = null;
    }
    if (log.isInfoEnabled()) {
      log.info(
          "Acquiring "+
          ((priorIdentity != null) ?
           ("transfered identity: "+priorIdentity) :
           ("new identity")));
    }
    myAgentIdService = (AgentIdentityService) 
      csb.getService(this, AgentIdentityService.class, null);
    if (myAgentIdService == null) {
      throw new RuntimeException(
          "Unable to get the agent identity service for agent "+
          getIdentifier());
    }
    try {
      myAgentIdService.acquire(priorIdentity);
    } catch (Exception e) {
      throw new RuntimeException(
          ("Unable to acquire agent identity for agent "+
           getIdentifier()+
           ((priorIdentity != null) ? 
            (" from transfered identity: "+
             priorIdentity) :
            (""))),
          e);
    }

    // fill in prior restart incarnation details
    if (persistenceData != null) {
      setRestartState(persistenceData.restartState);
    }

    loadRestartChecker();

    // get the Messenger instance from ClusterManagement
    if (log.isInfoEnabled()) {
      log.info("Registering with the message transport");
    }
    
    mtsClientAdapter = new MessageTransportClientAdapter();

    messenger = (MessageTransportService) 
      sb.getService(mtsClientAdapter, MessageTransportService.class, null);

    if (persistenceData != null) {
        org.cougaar.core.mts.AgentState t = persistenceData.mtsState;
        if (t != null) messenger.getAgentState().mergeAttributes(t);
    }

    messenger.registerClient(mtsClientAdapter);

    if (persistenceData != null) {
      // send all unsent messages
      List l = persistenceData.unsentMessages;
      if (l != null) {
        for (int i = 0, n = l.size(); i < n; i++) {
          Message cmi = (Message) l.get(i);
          sendMessage(cmi);
        }
      }
    }

    if (log.isInfoEnabled()) {
      log.info("Getting / adding all services");
    }

    // add alarm service
    ClusterServesClocks alarmClock = new AlarmClockAdapter(this);
    myAlarmServiceProvider = new AlarmServiceProvider(alarmClock);
    csb.addService(AlarmService.class, myAlarmServiceProvider);

    // add demo control
    ClusterServesClocks demoClock = new DemoClockAdapter(this);
    myDemoControlServiceProvider = new DemoControlServiceProvider(demoClock);
    csb.addService(DemoControlService.class, myDemoControlServiceProvider);

    // add a Service hook into the MessageSwitch
    myMessageSwitchSP = new MessageSwitchServiceProvider();
    csb.addService(MessageSwitchService.class, myMessageSwitchSP);

    //for backwards compatability
    super.loadInternalPriorityComponents();

    /** get the timers **/
    xTimer = (NaturalTimeService) 
      sb.getService(this, NaturalTimeService.class, null);
    rTimer = (RealTimeService) 
      sb.getService(this, RealTimeService.class, null);
  }

  protected void loadBinderPriorityComponents() {
    super.loadBinderPriorityComponents();
  }

  protected void loadComponentPriorityComponents() {
    super.loadComponentPriorityComponents();

    // get blackboard service
    myBlackboardService = (BlackboardForAgent) 
      getChildServiceBroker().getService(
          this, BlackboardForAgent.class, null);
    if (myBlackboardService == null) {
      throw new RuntimeException("Couldn't get BlackboardForAgent!");
    }
  }

  /** override hook to set up message handlers **/
  public void initialize() throws StateModelException {
    super.initialize();
    setupMessageHandlers();
  }


  /** Called object should start any threads it requires.
   *  Called object should transition to the ACTIVE state.
   *  @exception org.cougaar.util.StateModelException Cannot transition to new state.
   **/

  public void start() throws StateModelException {

    super.start();

    if (log.isInfoEnabled()) {
      log.info("Starting");
    }

    // start the message queue
    startQueueHandler();

    // do restart reconciliation if necessary
    startRestartChecker();

    // register for mobility
    if (myMobileAgentService == null) {
      myMobileAgentService = (MobileAgentService) 
        getServiceBroker().getService(
            this, MobileAgentService.class, null);
      if (myMobileAgentService == null) {
        if (log.isInfoEnabled()) {
          log.info("Not registered for agent mobility");
        }
      }
    }

    // children started as part of "super.add(..)".

    if (log.isInfoEnabled()) {
      log.info("Started");
    }

    if (eventService != null &&
        eventService.isEventEnabled()) {
      eventService.event(
          "AgentLifecycle("+"Started"+
          ") Agent("+getIdentifier()+
          ") Node("+localNode+
          ") Host("+localHost+
          ")");
    }
  }

  public void suspend() {
    if (log.isInfoEnabled()) {
      log.info("Suspending");
    }

    // suspend all children
    if (log.isInfoEnabled()) {
      log.info("Recursively suspending all child components");
    }
    super.suspend();

    if (log.isInfoEnabled()) {
      log.info("Suspending scheduler");
    }

    suspendRestartChecker();

    // shutdown the MTS
    if (log.isInfoEnabled()) {
      log.info("Shutting down message transport");
    }

    if (messenger != null) {
      messenger.unregisterClient(mtsClientAdapter);
    }

    stopQueueHandler();

    if (messenger != null) {
      // flush outgoing messages, block further input.
      // get a list of unsent messages.
      unsentMessages = messenger.flushMessages();

      // get MTS-internal state for this agent
      mtsState = messenger.getAgentState();

      // release messenger, remove agent name-server entry.
      getServiceBroker().releaseService(
          mtsClientAdapter, MessageTransportService.class, messenger);
      messenger = null;
    }

    if (moveTargetNode != null) {
      // moving, delay identity transfer until after getState()
      if (log.isInfoEnabled()) {
        log.info(
            "Postponing identity transfer to node "+
            moveTargetNode);
      }
    } else {
      // non-moving suspend?
      if (log.isInfoEnabled()) {
        log.info("Releasing identity");
      }
      myAgentIdService.release();
    }

    if (log.isInfoEnabled()) {
      log.info("Suspended");
    }

    if (persistenceService != null) {
      persistenceObject = myBlackboardService.getPersistenceObject();
      persistenceService.suspend();
    }

    if (moveTargetNode != null) {
      // record event
      if (eventService != null &&
          eventService.isEventEnabled()) {
        String moveTargetHost = "?";
        try {
          AddressEntry nodeEntry = 
            whitePagesService.get(
                moveTargetNode.getAddress(),
                "topology",
                10000); // wait at most 10 seconds
          if (nodeEntry != null) {
            moveTargetHost = nodeEntry.getURI().getHost();
          }
        } catch (Exception e) {
          if (log.isInfoEnabled()) {
            log.info(
                "Unable to get host for destination node "+
                moveTargetNode,
                e);
          }
        }
        eventService.event(
            "AgentLifecycle("+"Moving"+
            ") Agent("+getIdentifier()+
            ") Node("+localNode+
            ") Host("+localHost+
            ") ToNode("+moveTargetNode+
            ") ToHost("+moveTargetHost+
            ")");
      }
    }
  }

  public void resume() {
    if (log.isInfoEnabled()) {
      log.info("Resuming");
    }

    boolean acquiredIdentity = false;
    try {

      // re-establish our identity
      if (moveTargetNode != null) {
        // failed move, restart
        if (mobileIdentity != null) {
          // take and clear the saved identity
          TransferableIdentity tmp = 
            ((mobileIdentity != NULL_MOBILE_IDENTITY) ? 
             (mobileIdentity) : 
             null);
          mobileIdentity = null;
          if (log.isInfoEnabled()) {
            log.info(
                "Acquiring agent identify from"+
                " failed move to "+moveTargetNode+
                " and transfer-identity "+tmp);
          }
          try {
            myAgentIdService.acquire(tmp);
          } catch (Exception e) {
            throw new RuntimeException(
                "Unable to restart agent "+getIdentifier()+
                " after failed move to "+moveTargetNode+
                " and transfer-identity "+tmp, e);
          }
        } else {
          // never transfered identity (state capture failed?)
          if (log.isInfoEnabled()) {
            log.info(
                "Identity was never transfered to "+
                moveTargetNode);
          }
        }
      } else {
        // resume after non-move suspend
        if (log.isInfoEnabled()) {
          log.info(
              "Acquiring agent identify from scratch");
        }
        try {
          myAgentIdService.acquire(null);
        } catch (Exception e) {
          throw new RuntimeException(
              "Unable to resume agent "+getIdentifier()+
              " after non-move suspend", e);
        }
      }
      acquiredIdentity = true;

      // re-register MTS
      if (messenger == null) {
        if (log.isInfoEnabled()) {
          log.info(
              "Registering with the message transport service");
        }
        messenger = (MessageTransportService) 
          getServiceBroker().getService(
              mtsClientAdapter, MessageTransportService.class, null);

	if (mtsState != null) {
	  messenger.getAgentState().mergeAttributes(mtsState);
          mtsState = null;
        }
        
        messenger.registerClient(mtsClientAdapter);
      }

      if (log.isInfoEnabled()) {
        log.info("Resuming message transport");
      }

      startQueueHandler();

      // re-register for MessageTransport
      // send all unsent messages
      List l = unsentMessages;
      unsentMessages = null;
      for (int i = 0, n = ((l != null) ? l.size() : 0); i < n; i++) {
        Message cmi = (Message) l.get(i);
        sendMessage(cmi);
      }

      restartRestartChecker();

      // FIXME bug 989: resume alarm service

      // resume all children
      if (log.isInfoEnabled()) {
        log.info("Recursively resuming all child components");
      }
      super.resume();

    } catch (RuntimeException re) {
      if (acquiredIdentity) {
        // unable to resume, release identity
        myAgentIdService.release();
      }
      // should keep flags for other reverseable actions
      throw re;
    }
    
    if (log.isInfoEnabled()) {
      log.info("Resumed");
    }

    if (moveTargetNode != null) {
      if (eventService != null &&
          eventService.isEventEnabled()) {
        eventService.event(
            "AgentLifecycle("+"NotMoved"+
            ") Agent("+getIdentifier()+
            ") Node("+localNode+
            ") Host("+localHost+
            ") ToNode("+moveTargetNode+
            ")");
      }
      moveTargetNode = null;
    }
  }

  public void stop() {
    if (log.isInfoEnabled()) {
      log.info("Stopping");
    }

    stopRestartChecker();

    // disable mobility
    if (myMobileAgentService != null) {
      getServiceBroker().releaseService(
          this, MobileAgentService.class, myMobileAgentService);
      myMobileAgentService = null;
    }

    // should be okay...

    // stop all children
    if (log.isInfoEnabled()) {
      log.info("Recursively stopping all child components");
    }
    super.stop();

    // already transfered or released identity in "suspend()"

    if (log.isInfoEnabled()) {
      log.info("Stopped");
    }

    if (eventService != null &&
        eventService.isEventEnabled()) {
      if (moveTargetNode == null) {
        eventService.event(
            "AgentLifecycle("+"Stopped"+
            ") Agent("+getIdentifier()+
            ") Node("+localNode+
            ") Host("+localHost+
            ")");
      } else {
        eventService.event(
            "AgentLifecycle("+"Moved"+
            ") Agent("+getIdentifier()+
            ") Node("+localNode+
            ") Host("+localHost+
            ") ToNode("+moveTargetNode+
            ")");
      }
    }
  }

  public void unload() {
    if (log.isInfoEnabled()) {
      log.info("Unloading");
    }

    // unload in reverse order of "load()"

    ServiceBroker sb = getServiceBroker();
    ServiceBroker csb = getChildServiceBroker();

    // release child services
    csb.releaseService(this, BlackboardForAgent.class, myBlackboardService);

    csb.revokeService(MessageSwitchService.class, myMessageSwitchSP);
    csb.revokeService(DemoControlService.class, myDemoControlServiceProvider);
    csb.revokeService(AlarmService.class, myAlarmServiceProvider);

    csb.releaseService(
        this, AgentIdentityService.class, myAgentIdService);

    csb = null;

    // unload children
    if (log.isInfoEnabled()) {
      log.info("Recursively unloading all child components");
    }
    super.unload();

    //
    // release context-based services
    //

    if (log.isInfoEnabled()) {
      log.info("Releasing / revoking all services");
    }

    sb.releaseService(this, RealTimeService.class, rTimer);
    sb.releaseService(this, NaturalTimeService.class, xTimer);

    //
    // release remaining services
    //

    // messenger already released in "suspend()"

    // remove ourselves from the VM-local context
    if (log.isDebugEnabled()) {
      log.debug("Removing from the cluster context table");
    }
    ClusterContextTable.removeContext(getMessageAddress());

    unloadRestartChecker();

    if (log.isInfoEnabled()) {
      log.info("Unloaded");
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = LoggingService.NULL;
    }

    // optionally spawn a thread to watch for clean agent removal
    Long removalCheckMillis = Long.getLong(
        "org.cougaar.core.agent.removalCheckMillis");
    if (removalCheckMillis != null) {
      AgentRemovalChecker arc = 
        new AgentRemovalChecker(
            this, removalCheckMillis.longValue());
      // FIXME should use threading service, but already released it!
      Thread arct = 
        new Thread(arc, getIdentifier()+" Agent removal checker");
      arct.start();
    }
  }

  /**
   * Get the state of this agent, which should be suspended.
   */
  public Object getState() {
    return persistenceObject;
  }

  private List getPersistenceData() {
    PersistenceData persistenceData = new PersistenceData();

    // get the child components
    persistenceData.descs = super.captureState();

    if (moveTargetNode != null) {
      // Moving state capture
      // get unsent messages
      persistenceData.unsentMessages = unsentMessages;

      // get remote incarnations
      persistenceData.restartState = getRestartState();

      // get transferrable identity
      if (log.isInfoEnabled()) {
        log.info("Transfering identity from node "+
                 localNode+" to node "+moveTargetNode);
      }
      persistenceData.identity =
        myAgentIdService.transferTo(moveTargetNode);
      if (persistenceData.identity == null) {
        persistenceData.identity = NULL_MOBILE_IDENTITY;
      }
    } else {
      // non-moving state capture
      persistenceData.identity = null;
    }
    persistenceData.agentId = getMessageAddress();
    persistenceData.mtsState = mtsState;
    List d = new ArrayList(1);
    d.add(persistenceData);
    return d;
  }

  public void setState(Object loadState) {
    this.persistenceObject = (PersistenceObject) loadState;
  }

  public String getIdentifier() {
    return myMessageAddress_.getAddress();
  }

  //
  // Agent identity client
  //

  public String getName() {
    return getIdentifier();
  }

  public void identityRevoked(CrlReason reason) {
    if (log.isWarnEnabled()) {
      log.warn("Identity has been revoked: "+reason);
    }
    // ignore for now, re-acquire or die TBA
  }

  //
  // Mobility listener
  //

  public MessageAddress getAddress() {
    return getMessageAddress();
  }

  public void onDispatch(MessageAddress destinationNode) {
    if (log.isInfoEnabled()) {
      log.info(
          "Preparing for move from node "+localNode+
          " to node "+destinationNode);
    }
    // save target node for later "suspend()" and "getState()" use
    moveTargetNode = destinationNode;
  }

  // 
  // MessageTransportClient
  //

  public MessageAddress getMessageAddress() {
    return myMessageAddress_;
  }

  //-----------------------------------------------------------------
  // message switch
  //-----------------------------------------------------------------

  /** MessageSwitch is a MessageHandler which calls an ordered 
   * list of other MessageHandler instances in order until 
   * one returns a true value from handle.
   **/
  protected class MessageSwitch implements MessageHandler {
    /** List of MessageHandler instances **/
    private final List handlers = new ArrayList(11);

    public boolean handleMessage(Message m) {
      synchronized (handlers) {
        for (int i=0, l=handlers.size(); i<l; i++) {
          MessageHandler h = (MessageHandler) handlers.get(i);
          if (h.handleMessage(m)) return true;
        }
      }
      return false;
    }

    public void addMessageHandler(MessageHandler mh) {
      synchronized (handlers) {
        handlers.add(mh);
      }
    }
    public void removeMessageHandler(MessageHandler mh) {
      synchronized (handlers) {
        handlers.remove(mh);
      }
    }
  }

  private MessageHandler rawMessageHandler;

  /** Called during initialize() to set up message handlers **/
  private void setupMessageHandlers() {
    rawMessageHandler = setupRawMessageHandler();
  }

  private MessageSwitch rawMessageSwitch = null;
  /** return a reference to the low-level message switch. **/
  protected MessageSwitch getMessageSwitch() { return rawMessageSwitch; }

  /** Called to initialize the primary MessageHandler which is used to 
   * process incoming messages prior to adding to the agent's messagequeue.
   * MessageHandlers here will be run in the MTS's thread of execution, <em>not</em>
   * the agent's thread.
   * <br>
   * The default method constructs and initializes the rawMessageSwitch (see getMessageSwitch())
   * adding handlers to deal with ClusterMessages as well as a number of infrastructure
   * messages.
   * @note To allow subcomponents access to raw messages, extending methods
   * should make sure to call this method (e.g. with super.setupRawMessageHandler) and
   * make sure the the returned value is used appropriately.
   **/
  protected MessageHandler setupRawMessageHandler() {
    rawMessageSwitch = new MessageSwitch();
    rawMessageSwitch.addMessageHandler(new MessageHandler() {
        public boolean handleMessage(Message message) {
          if (message instanceof ClusterMessage) {
            MessageAddress orig = message.getOriginator();
            receivedMessageFrom(orig);
          }
          return false;         // don't ever consume it
        }
      });
    if (showTraffic) {
      rawMessageSwitch.addMessageHandler(new MessageHandler() {
          public boolean handleMessage(Message message) {
            showProgress("-");
            return false;         // don't ever consume it
          }
        });
    }
    rawMessageSwitch.addMessageHandler(new MessageHandler() {
        public boolean handleMessage(Message message) {
          if (message instanceof AdvanceClockMessage) {
            handleAdvanceClockMessage((AdvanceClockMessage)message);
            return true;
          } else {
            return false;
          }
        }
      });
    // this one should go away
    rawMessageSwitch.addMessageHandler(new MessageHandler() {
        public boolean handleMessage(Message message) {
          if (message instanceof ComponentMessage) {
            handleComponentMessage((ComponentMessage)message);
            return true;
          } else {
            return false;
          }
        }
      });
    rawMessageSwitch.addMessageHandler(new MessageHandler() {
        public boolean handleMessage(Message message) {
          if (message instanceof ClusterMessage) {
            // internal message queue
            getQueueHandler().addMessage((ClusterMessage)message);
            return true;
          } else {
            return false;
          }
        }
      });

    return rawMessageSwitch;
  }

  /** handle a ComponentMessage.  Probably a bad idea nowadays. **/
  private void handleComponentMessage(ComponentMessage cm) {
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
          "Unsupported ComponentMessage: "+cm);
    }
  }

  /** Deal with a message received directly from the MTS.
   * Just calls the rawMessageHandler
   **/
  public void receiveMessage(Message message) {
    try {
      boolean handled = rawMessageHandler.handleMessage(message);
      if (!handled) {
        log.warn("Received unhandled Message ("+
                  message.getClass()+"): "+message);
      }
    } catch (Exception e) {
      log.error("Uncaught Exception while handling Message ("+message.getClass()+"): "+message, e);
    }
  }

  private final void receiveQueuedMessages(List messages) {
    try {
      myBlackboardService.receiveMessages(messages);
    } catch (Exception e) {
      log.error("Uncaught Exception while handling Queued Messages", e);
    }
  }

  //-----------------------------------------------------------------
  // restart checker
  //-----------------------------------------------------------------

  /** call when a message is received */
  private void receivedMessageFrom(MessageAddress originator) {
    recordAddress(originator);
  }
  
  /** call when a message is sent */
  private void sentMessageTo(MessageAddress target) {
    recordAddress(target);
  }

  private void loadRestartChecker() {
    ServiceBroker sb = getServiceBroker();

    whitePagesService = (WhitePagesService) 
      sb.getService(this, WhitePagesService.class, null);

    try {
      bindRestart();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to load restart checker", e);
    }
  }

  private void startRestartChecker() {
    restart();
    startRestartTimer();
  }

  private void suspendRestartChecker() {
    stopRestartTimer();
  }

  private void restartRestartChecker() {
    startRestartTimer();
  }

  private void stopRestartChecker() {
    stopRestartTimer();
  }

  private void unloadRestartChecker() {
    ServiceBroker sb = getServiceBroker();
    sb.releaseService(
        this, WhitePagesService.class, whitePagesService);
  }

  private RestartState getRestartState() {
    Map m;
    synchronized (incarnationMap) {
      m = new HashMap(incarnationMap);
    }
    return new RestartState(
        getMessageAddress(),
        incarnation,
        moveId,
        m);
  }

  private void setRestartState(RestartState rs) {
    if (rs != null) {
      needsRestart = false;
      incarnation = rs.incarnation;
      synchronized (incarnationMap) {
        incarnationMap.putAll(
            rs.incarnationMap);
      }
      // ignore moveId, maybe use someday
    }
  }

  private void bindRestart() throws Exception {
    final LoggingService ls = log;
    Callback callback = new Callback() {
        public void execute(Response res) {
          if (res.isSuccess()) {
            if (ls.isInfoEnabled()) {
              ls.info("WP Response: "+res);
            }
          } else {
            ls.error("WP Error: "+res);
          }
        }
      };

    // register WP version numbers
    if (log.isInfoEnabled()) {
      log.info("Updating white pages");
    }
    if (incarnation == 0) {
      incarnation = System.currentTimeMillis();
    }
    moveId = System.currentTimeMillis();
    // ignore prior moveId
    URI versionURI = 
      URI.create("version:///"+incarnation+"/"+moveId);
    AddressEntry versionEntry = 
      AddressEntry.getAddressEntry(
          getIdentifier(),
          "version",
          versionURI);
    whitePagesService.rebind(versionEntry, callback); // should really pay attention

    // register WP node location
    URI nodeURI = 
      URI.create("node://"+localHost+"/"+localNode.getAddress());
    AddressEntry nodeEntry = 
      AddressEntry.getAddressEntry(
          getIdentifier(),
          "topology",
          nodeURI);
    whitePagesService.rebind(nodeEntry, callback); // really should pay attention
  }

  /**
   * Get the latest incarnation number for the specified agent.
   *
   * @return -1 if the WP lacks a version entry for the agent
   */
  private long lookupCurrentIncarnation(
      MessageAddress agentId) throws Exception {
    AddressEntry versionEntry = 
      whitePagesService.get(agentId.getAddress(), "version");
    if (versionEntry == null) {
      return -1;
    }
    URI uri = versionEntry.getURI();
    try {
      String p = uri.getRawPath();
      int i = p.indexOf('/', 1);
      String s = p.substring(1, i);
      return Long.parseLong(s);
    } catch (Exception e) {
      throw new RuntimeException(
          "Malformed incarnation uri: "+uri, e);
    }
  }

  private void startRestartTimer() {
    if (restartTimer == null) {
      restartTimer = new Timer();
      TimerTask tTask = 
        new TimerTask() {
          public void run() {
            checkRestarts();
          }
        };
      restartTimer.schedule(
          tTask,
          RESTART_CHECK_INTERVAL,
          RESTART_CHECK_INTERVAL);
    }
  }

  private void stopRestartTimer() {
    if (restartTimer != null) {
      restartTimer.cancel();
      // note: If timer is running now then blackboard.restartAgent
      // may compain about the messenger being disabled.  This can be
      // ignored.
      restartTimer = null;
    }
  }

  /**
   * Periodically called to check remote agent restarts.
   * <p>
   * The incarnation map has an entry for every agent that we have
   * communicated with.  The value is the last known incarnation number
   * of the agent.
   * <p>
   * The first time we check restarts, we have ourself restarted so we
   * proceed to verify our state against _all_ the other agents. We do
   * this because we have no record with whom we have been in
   * communication. In this case, we notify the blackboard, which will
   * instruct the domains to reconcile the objects in the local
   * blackboard.  Messages will be sent only to those agents for which
   * we have communicated with.  The sending of those messages will add
   * entries to the restart incarnation map. So after doing a restart 
   * with an agent if there is an entry in the map for that agent, we set
   * the saved incarnation number to the current value for that agent.
   * This avoids repeating the restart later. If the current incarnation
   * number differs, the agent must have restarted so we initiate the
   * restart reconciliation process.
   */
  private void checkRestarts() {
    if (VERBOSE_RESTART && log.isDebugEnabled()) {
      log.debug("Check restarts");
    }
    // snapshot the incarnation map
    Map restartMap;
    synchronized (incarnationMap) {
      if (incarnationMap.isEmpty()) {
        return; // nothing to do
      }
      restartMap = new HashMap(incarnationMap);
    }
    // get the latest incarnations from the white pages
    List reconcileList = null;
    for (Iterator iter = restartMap.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      MessageAddress agentId = (MessageAddress) me.getKey();
      long cachedInc = ((Long) me.getValue()).longValue();
      long currentInc;
      try {
        currentInc = lookupCurrentIncarnation(agentId);
      } catch (Exception e) {
        if (log.isInfoEnabled()) {
          log.info("Failed restart check for "+agentId, e);
        }
        // pretend that it hasn't changed; we'll pick it
        // up the next time around
        currentInc = cachedInc;
      }
      if (VERBOSE_RESTART && log.isDebugEnabled()) {
        log.debug(
            "Update agent "+agentId+
            " incarnation from "+cachedInc+
            " to "+currentInc);
      }
      if (currentInc > 0 && currentInc != cachedInc) {
        Long l = new Long(currentInc);
        synchronized (incarnationMap) {
          incarnationMap.put(agentId, l);
        }
        if (cachedInc > 0) {
          // must reconcile with this agent
          if (reconcileList == null) {
            reconcileList = new ArrayList();
          }
          reconcileList.add(agentId);
        }
      }
    }
    // reconcile with any agent (that we've communicated with) that
    // has a new incarnation number
    int n = (reconcileList == null ? 0 : reconcileList.size());
    for (int i = 0; i < n; i++) {
      MessageAddress agentId = (MessageAddress) reconcileList.get(i);
      if (log.isInfoEnabled()) {
        log.info(
            "Detected (re)start of agent "+agentId+
            ", synchronizing blackboards");
      }
      myBlackboardService.restartAgent(agentId);
    }
  }

  /**
   * The local agent has restarted.
   */
  private void restart() {
    if (!(needsRestart)) {
      if (log.isInfoEnabled()) {
        log.info("No restart blackboard synchronization required");
      }
      return;
    }
    needsRestart = false;
    if (log.isInfoEnabled()) {
      log.info("Restarting, synchronizing blackboards");
    }
    try {
      // restart this agent.  The "null" is shorthand for 
      // "all agents that are not this agent".
      myBlackboardService.restartAgent(null);
    } catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info("Restart failed", e);
      }
    }
  }

  /*
   * Insure that we are tracking incarnations number for the agent at
   * a given address. If the specified agent is not in the restart
   * incarnation map it means we have never before communicated with that 
   * agent or we have just restarted and are sending restart messages. In 
   * both cases, it is ok to store the special "unknown incarnation" marker
   * because we do not want to detect any restart.
   **/
  private void recordAddress(MessageAddress agentId) {
    // only include agent addresses in restart checking
    synchronized (incarnationMap) {
      if (incarnationMap.get(agentId) == null) {
        if (VERBOSE_RESTART && log.isDebugEnabled()) {
          log.debug("Adding "+agentId+" to restart table");
        }
        incarnationMap.put(agentId, new Long(0L));
      }
    }
  }

  private static class RestartState implements java.io.Serializable {

    private final MessageAddress agentId;
    private final long incarnation;
    private final long moveId;
    private final Map incarnationMap;

    public RestartState(
        MessageAddress agentId,
        long incarnation,
        long moveId,
        Map incarnationMap) {
      this.agentId = agentId;
      this.incarnation = incarnation;
      this.moveId = moveId;
      this.incarnationMap = incarnationMap;
      if ((agentId == null) ||
          (incarnationMap == null)) {
        throw new IllegalArgumentException("null param");
      }
    }

    public String toString() {
      return 
        "Agent "+agentId+
        ", incarnation "+incarnation+
        ", moveId "+moveId;
    }

    private static final long serialVersionUID = 1273891827367182983L;
  }

  //-----------------------------------------------------------------
  // message switch + queue handler
  //-----------------------------------------------------------------
 
  // required by the interface - ClusterMessage should really go away. Ugh.
  private void sendMessage(Message message)
  {
    sentMessageTo(message.getTarget());
    if (showTraffic) showProgress("+");
    try {
      if (messenger == null) {
        throw new RuntimeException(
            "MessageTransport unavailable. Message dropped.");
      } 
      messenger.sendMessage(message);
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error("Problem sending message", ex);
      }
    }
  }

  //
  // Queue Handler
  //

  private QueueHandler queueHandler = null;

  private boolean isQueueHandlerStarted = false;

  private Object queueHandlerLock = new Object ();

  private void startQueueHandler() {
    synchronized (queueHandlerLock) {
      if (! isQueueHandlerStarted ) {
        getQueueHandler().start();
        isQueueHandlerStarted = true;
      } else {
        if (log.isErrorEnabled()) {
          log.error("Illegal restart of QueueHandler");
        }
      }
    }
  }

  private void stopQueueHandler() {
    synchronized (queueHandlerLock) {
      if (isQueueHandlerStarted ) {
        getQueueHandler().halt();
        isQueueHandlerStarted = false;
        queueHandler = null;
      } else {
        if (log.isErrorEnabled()) {
          log.error("Illegal stop of QueueHandler");
        }
      }
    }
  }

  private QueueHandler getQueueHandler() {
    synchronized (queueHandlerLock) {
      if (queueHandler == null) {
        queueHandler = new QueueHandler(this);
      }
      return queueHandler;
    }
  }

  private static class QueueHandler extends Thread {
    private SimpleAgent cluster;
    private List queue = new ArrayList();
    private List msgs = new ArrayList();
    private boolean running = true;
    public QueueHandler(SimpleAgent cluster) {
      super(cluster.getMessageAddress()+"/RQ");
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
        if (!running) {
          System.err.println(
              "Queue is not running, message will be ignored!");
        }

        queue.add(m);
        queue.notify();
      }
    }
  }

  private void handleAdvanceClockMessage(AdvanceClockMessage acm) {
    xTimer.setParameters(acm.getParameters());
  }

  private static void showProgress(String p) {
    if (!isQuiet) {
      // Too many threads in a multi-cluster node are printing progress 
      // at the same time and we don't really care about the newlines
      // so we'll drop the synchronized and live with the consequences.
      Logging.printDot(p);
    }
  }

  //-----------------------------------------------------------------
  // clock services
  //-----------------------------------------------------------------

  /**
   * Adapter for agent internal access.
   * <p>
   * This is used as a base-class in agent-internal
   * ComponentDescriptions that require a pointer back into
   * the agent.  During load these references will have the
   * "setAgent(..)" method called with the SimpleAgent
   * instance.
   * <p>
   * In the future this will be turned into a public class,
   * perhaps a generic component model capability...
   */
  private static class InternalAdapter 
    implements Serializable {
      private transient SimpleAgent sa;
      public InternalAdapter(SimpleAgent sa) {
        setAgent(sa);
      }
      public void setAgent(SimpleAgent sa) {
        this.sa = sa;
      }
      public SimpleAgent getAgent() {
        return sa;
      }
    }

  // containment view of this agent
  private static class AgentContainmentAdapter
    extends InternalAdapter
    implements AgentContainmentService {
      public AgentContainmentAdapter(SimpleAgent sa) {
        super(sa);
      }
      public boolean add(ComponentDescription desc) {
        return getAgent().add(desc);
      }
      public boolean remove(ComponentDescription desc) {
        return getAgent().remove(desc);
      }
      public boolean contains(ComponentDescription desc) {
        return getAgent().contains(desc);
      }
    }

  // alarm clock view of this agent
  private static class AlarmClockAdapter
    extends InternalAdapter
    implements ClusterServesClocks {
      public AlarmClockAdapter(SimpleAgent sa) {
        super(sa);
      }
      // alarm service:
      public long currentTimeMillis() {
        return getAgent().xTimer.currentTimeMillis();
      }
      public void addAlarm(Alarm alarm) {
        getAgent().xTimer.addAlarm(alarm);
      }
      public void addRealTimeAlarm(Alarm alarm) {
        getAgent().rTimer.addAlarm(alarm);
      }
      // demo service:
      private void die() { throw new UnsupportedOperationException(); }
      public MessageAddress getMessageAddress() { die(); return null; }
      public void setTime(long time) { die(); }
      public void setTime(long time, boolean leaveRunning) { die(); }
      public void setTimeRate(double newRate) { die(); }
      public void advanceTime(long timePeriod) { die(); }
      public void advanceTime(long timePeriod, boolean leaveRunning) { die(); }
      public void advanceTime(long timePeriod, double newRate) { die(); }
      public void advanceTime(ExecutionTimer.Change[] changes) { die(); }
      public double getExecutionRate() { die(); return -1; }
    }

  // alarm clock view of this agent
  private static class DemoClockAdapter
    extends InternalAdapter
    implements ClusterServesClocks {
      public DemoClockAdapter(SimpleAgent sa) {
        super(sa);
      }
      // alarm service:
      private void die() { throw new UnsupportedOperationException(); }
      public long currentTimeMillis() { die(); return -1; }
      public void addAlarm(Alarm alarm) { die(); }
      public void addRealTimeAlarm(Alarm alarm) { die(); }
      // demo service:
      public MessageAddress getMessageAddress() {
        return getAgent().getMessageAddress();
      }
      public void setTime(long time) {
        sendAdvanceClockMessage(
            time, true, 0.0, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void setTime(long time, boolean running) {
        sendAdvanceClockMessage(
            time, true, 0.0, running, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void setTimeRate(double newRate) {
        sendAdvanceClockMessage(
            0L, false, newRate, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceTime(long timePeriod){
        sendAdvanceClockMessage(
            timePeriod, false, 0.0, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceTime(long timePeriod, boolean running){
        sendAdvanceClockMessage(
            timePeriod, false, 0.0, running, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceTime(long timePeriod, double newRate){
        sendAdvanceClockMessage(
            timePeriod, false, newRate, false, NaturalTimeService.DEFAULT_CHANGE_DELAY);
      }
      public void advanceTime(ExecutionTimer.Change[] changes) {
        ExecutionTimer.Parameters[] params = getAgent().xTimer.createParameters(changes);
        for (int i = 0; i < params.length; i++) {
          sendAdvanceClockMessage(params[i]);
        }
      }
      public double getExecutionRate() {
        return getAgent().xTimer.getRate();
      }
      private void sendAdvanceClockMessage(long millis,
          boolean millisIsAbsolute,
          double newRate,
          boolean forceRunning,
          long changeDelay)
      {
        ExecutionTimer.Parameters newParameters =
          getAgent().xTimer.createParameters(
              millis, millisIsAbsolute, newRate,
              forceRunning, changeDelay);
        sendAdvanceClockMessage(newParameters);
      }
      private void sendAdvanceClockMessage(
          ExecutionTimer.Parameters newParameters) {
        MessageAddress a = getMessageAddress();
        AdvanceClockMessage acm =
          new AdvanceClockMessage(a, newParameters);
        sendAdvanceClockMessage(acm);
      }
      private void sendAdvanceClockMessage(AdvanceClockMessage acm) {
        // use MessageSwitchService?
        getAgent().sendMessage(acm);
      }
    }

  /**
   * Runner to check for agent GC.
   * <p>
   * Note that the agent may be GC'ed and child components of the 
   * agent may not be GC'ed, e.g. due to statics or back-pointers
   * with the node.
   * <p>
   * This will likely be moved into the agent manager at some point...
   */
  private static class AgentRemovalChecker implements Runnable {

    private final WeakReference agentRef;
    private final long waitMillis;

    public AgentRemovalChecker(
        SimpleAgent agent,
        long waitMillis) {
      agentRef = new WeakReference(agent);
      this.waitMillis = waitMillis;
    }

    public void run() {
      // wait half the time
      try {
        Thread.sleep(waitMillis >> 1);
      } catch (InterruptedException ie) {
      }
      SimpleAgent agent = (SimpleAgent) agentRef.get();
      if (agent == null) {
        // great!
        return;
      }
      // force a GC and wait again
      System.gc();
      try {
        Thread.sleep(waitMillis >> 1);
      } catch (InterruptedException ie) {
      }
      agent = (SimpleAgent) agentRef.get();
      if (agent == null) {
        // great!
        return;
      }
      // create error message
      Logging.getLogger(agent).warn(
          "Unload of agent "+agent.getIdentifier()+
          " didn't result in garbage collection of the agent,"+
          " please use a profiler to check for a memory leak");
    }
  }

  //-----------------------------------------------------------------
  // state object
  //-----------------------------------------------------------------

  private static class PersistenceData implements java.io.Serializable {

    private MessageAddress agentId;
    private TransferableIdentity identity;
    private ComponentDescriptions descs;
    private org.cougaar.core.mts.AgentState mtsState;
    private List unsentMessages; // List<ClusterMessage>
    private RestartState restartState;

    public String toString() {
      return 
        "Agent "
        + agentId
        + " state, identity "
        + identity
        + ", descs "
        + descs
        + ", mtsState "
        + (mtsState != null)
        + ", unsentMessages["
        + (unsentMessages == null ?
            "none" :
            String.valueOf(unsentMessages.size()))
        + "]";
    }

    private static final long serialVersionUID = 3109298128098682091L;
  }

  //-----------------------------------------------------------------
  // message transport requestor
  //-----------------------------------------------------------------

  private class MessageTransportClientAdapter implements MessageTransportClient {
    public void receiveMessage(Message message) {
      SimpleAgent.this.receiveMessage(message);
    }

    public MessageAddress getMessageAddress() {
      return SimpleAgent.this.getMessageAddress();
    }
  }

  //-----------------------------------------------------------------
  // message switch
  //-----------------------------------------------------------------

  // implement the MessageSwitch Service
  private class MessageSwitchServiceProvider implements ServiceProvider {
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (MessageSwitchService.class.isAssignableFrom(serviceClass)) {
        return new MessageSwitchServiceImpl();
      } else {
        return null;
      }
    }
    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) { }
  }

  private class MessageSwitchServiceImpl implements MessageSwitchService {
    public void sendMessage(Message m) {
      SimpleAgent.this.sendMessage(m);
    }
    public void addMessageHandler(MessageHandler mh) {
      SimpleAgent.this.getMessageSwitch().addMessageHandler(mh);
    }
    public MessageAddress getMessageAddress() {
      return SimpleAgent.this.getMessageAddress();
    }
  }

}
