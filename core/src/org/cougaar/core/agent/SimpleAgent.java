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

package org.cougaar.core.agent;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
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
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.BoundComponent;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mobility.MobileAgentService;
import org.cougaar.core.mts.AgentState;
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
import org.cougaar.core.service.AgentContainmentService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.DemoControlService;
import org.cougaar.core.service.IntraAgentMessageTransportService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.TopologyEntry; // inlined
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.core.service.TopologyWriterService;
import org.cougaar.core.service.identity.AgentIdentityClient;
import org.cougaar.core.service.identity.AgentIdentityService;
import org.cougaar.core.service.identity.CrlReason;
import org.cougaar.core.service.identity.TransferableIdentity;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.StateModelException;

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
  implements 
  AgentIdentityClient,
  StateObject
{
  // this node's address
  private MessageAddress localNode;

  // incarnation for this agent, which is incremented every time
  // this agent restarts but not when the agent moves.
  private long incarnation;

  // move identity of this agent, which is incremented every time this
  // agent moves.
  private long moveId;

  // state for this agent if it is arriving from a move
  // this is set in "setState(..)" and used within "load()"
  private AgentState agentState;

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

  private LoggingService log = LoggingService.NULL;

  private AgentIdentityService myAgentIdService;

  private TopologyReaderService myTopologyReaderService;
  private TopologyWriterService myTopologyWriterService;

  private MessageTransportService messenger;
  /** stub for keeping the MTS client at a distance **/
  private MessageTransportClient mtsClientAdapter;

  private MobileAgentService myMobileAgentService;

  private NaturalTimeService xTimer;
  private RealTimeService rTimer;
  private AlarmServiceProvider myAlarmServiceProvider;
  private DemoControlServiceProvider myDemoControlServiceProvider;
  private ServiceProvider myMessageSwitchSP;

  private BlackboardForAgent myBlackboardService;

  private NamingService myNamingService;

  // map of agent name to most recently observed incarnation, used
  // to detect the restart of remote agents, which requires a
  // resync beteen this agent and the restarted agent.
  private boolean needsRestart = true;
  private final Map restartIncarnationMap = new HashMap();

  // properties
  private static final boolean VERBOSE_RESTART = false;
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

  /** Get the components from the ComponentInitializerService or the state **/
  protected ComponentDescriptions findExternalComponentDescriptions() {

    if (agentState != null) {
      // get descriptions from mobile state
      List tuples = agentState.tuples;
      // fix tuples where the desc.getParamater is an 
      // "InternalAdapter", since these are pointers into *this*
      // agent.  This is required for agent mobility to work.
      for (int i=0, n=tuples.size(); i<n; i++) {
        StateTuple st = (StateTuple) tuples.get(i);
        ComponentDescription desc = st.getComponentDescription();
        Object p = desc.getParameter();
        if (p instanceof InternalAdapter) {
          if (log.isInfoEnabled()) {
            log.info("Setting agent back-pointer in "+desc);
          }
          ((InternalAdapter)p).setAgent(this);
        }
      }
      return new ComponentDescriptions(tuples);
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
      List l = new ArrayList(cds.length + 13);

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

      // share our mts with the blackboard
      l.add(new ComponentDescription(
            getMessageAddress()+"IntraMTS",
            Agent.INSERTION_POINT + ".IntraMTS",
            "org.cougaar.core.agent.service.iamts.IntraAgentMessageTransportServiceComponent",
            null,
            (new IntraAgentMTSAdapter(this)), // access to *this* agent's mts + addr
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

    // do the standard thing.
    super.load();

    // release load-time agent state for GC
    this.agentState = null;

    if (log.isInfoEnabled()) {
      log.info("Loaded");
    }
  }

  protected void loadHighPriorityComponents() {
    ServiceBroker sb = getServiceBroker();

    // validate the state
    if (agentState != null) {
      // verify state's agent id
      MessageAddress agentId = agentState.agentId;
      if (!(getMessageAddress().equals(agentId))) {
        if (log.isErrorEnabled()) {
          log.error(
              "Load state contains incorrect agent address "+agentId);
        }
        // continue anyways
      }
    }

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

    // acquire our identity
    TransferableIdentity priorIdentity = 
      ((agentState != null) ?
       agentState.identity :
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
      sb.getService(this, AgentIdentityService.class, null);
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

    myTopologyReaderService = (TopologyReaderService) 
      sb.getService(this, TopologyReaderService.class, null);
    myTopologyWriterService = (TopologyWriterService) 
      sb.getService(this, TopologyWriterService.class, null);

    // register in the topology
    if (log.isInfoEnabled()) {
      log.info("Updating topology entry to \"active\"");
    }
    int topologyType = 
      ((getMessageAddress().equals(localNode)) ?
       (TopologyEntry.NODE_AGENT_TYPE) :
       (TopologyEntry.AGENT_TYPE));
    moveId = System.currentTimeMillis();
    if (agentState != null) {
      // resume the prior incarnation number
      incarnation = agentState.incarnation;
      long priorMoveId = agentState.moveId;
      myTopologyWriterService.updateAgent(
          getIdentifier(),
          topologyType,
          incarnation,
          moveId,
          TopologyEntry.ACTIVE,
          priorMoveId);
    } else {
      // create a new or restarted agent entry
      incarnation = System.currentTimeMillis();
      myTopologyWriterService.createAgent(
          getIdentifier(),
          topologyType,
          incarnation,
          moveId,
          TopologyEntry.ACTIVE);
    }

    // add our address to our VM's cluster table
    if (log.isDebugEnabled()) {
      log.debug("Adding to the cluster context table");
    }
    ClusterContextTable.addContext(getMessageAddress());

    // fill in prior restart incarnation details
    if (agentState != null) {
      setRestartState(agentState.restartState);
    }

    // get the Messenger instance from ClusterManagement
    if (log.isInfoEnabled()) {
      log.info("Registering with the message transport");
    }
    
    mtsClientAdapter = new MessageTransportClientAdapter();

    messenger = (MessageTransportService) 
      sb.getService(mtsClientAdapter, MessageTransportService.class, null);

    if (agentState != null) {
        org.cougaar.core.mts.AgentState t = agentState.mtsState;
	messenger.getAgentState().mergeAttributes(t);
    }

    messenger.registerClient(mtsClientAdapter);

    if (agentState != null) {
      // send all unsent messages
      List l = agentState.unsentMessages;
      for (int i = 0, n = l.size(); i < n; i++) {
        Message cmi = (Message) l.get(i);
        sendMessage(cmi);
      }
    }

    if (log.isInfoEnabled()) {
      log.info("Getting / adding all services");
    }

    // add alarm service
    ClusterServesClocks alarmClock = new AlarmClockAdapter(this);
    myAlarmServiceProvider = new AlarmServiceProvider(alarmClock);
    sb.addService(AlarmService.class, myAlarmServiceProvider);

    // add demo control
    ClusterServesClocks demoClock = new DemoClockAdapter(this);
    myDemoControlServiceProvider = new DemoControlServiceProvider(demoClock);
    sb.addService(DemoControlService.class, myDemoControlServiceProvider);

    // add a Service hook into the MessageSwitch
    myMessageSwitchSP = new MessageSwitchServiceProvider();
    sb.addService(MessageSwitchService.class, myMessageSwitchSP);

    //for backwards compatability
    super.loadInternalPriorityComponents();

    /** get the timers **/
    xTimer = (NaturalTimeService) sb.getService(this, NaturalTimeService.class, null);
    rTimer = (RealTimeService) sb.getService(this, RealTimeService.class, null);
  }

  protected void loadBinderPriorityComponents() {
    super.loadBinderPriorityComponents();
  }

  protected void loadComponentPriorityComponents() {
    super.loadComponentPriorityComponents();

    ServiceBroker sb = getServiceBroker();

    // get blackboard service
    myBlackboardService = (BlackboardForAgent) 
      sb.getService(this, BlackboardForAgent.class, null);
    if (myBlackboardService == null) {
      throw new RuntimeException("Couldn't get BlackboardForAgent!");
    }

    // get naming service
    myNamingService = (NamingService) 
      sb.getService(this, NamingService.class, null);
    if (myNamingService == null) {
      throw new RuntimeException("Couldn't get NamingService!");
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
    restart();

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
  }


  public void suspend() {
    super.suspend();

    if (log.isInfoEnabled()) {
      log.info("Suspending");
    }

    // suspend all children
    if (log.isInfoEnabled()) {
      log.info("Recursively suspending all child components");
    }
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.suspend();
    }

    if (log.isInfoEnabled()) {
      log.info("Suspending scheduler");
    }

    // FIXME bug 989: cancel all alarms

    stopRestartChecker();

    // update the topology 
    if (moveTargetNode != null) {
      if (log.isInfoEnabled()) {
        log.info("Updating topology entry to \"moving\"");
      }
      int topologyType = 
        ((getMessageAddress().equals(localNode)) ?
         (TopologyEntry.NODE_AGENT_TYPE) :
         (TopologyEntry.AGENT_TYPE));
      myTopologyWriterService.updateAgent(
          getIdentifier(),
          topologyType,
          incarnation,
          moveId,
          TopologyEntry.MOVING,
          moveId);
    } else {
      if (log.isInfoEnabled()) {
        log.info("Removing topology entry");
      }
      myTopologyWriterService.removeAgent(getIdentifier());
    }

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
  }

  public void resume() {
    super.resume();

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

      // FIXME for now, re-register MTS prior to topology
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

      // move failed, re-aquire the topology entry
      if (log.isInfoEnabled()) {
        log.info("Updating topology entry to \"active\"");
      }
      int topologyType = 
        ((getMessageAddress().equals(localNode)) ?
         (TopologyEntry.NODE_AGENT_TYPE) :
         (TopologyEntry.AGENT_TYPE));
      long priorMoveId = moveId;
      ++moveId;
      myTopologyWriterService.updateAgent(
          getIdentifier(),
          topologyType,
          incarnation,
          moveId,
          TopologyEntry.ACTIVE,
          priorMoveId);

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

      startRestartChecker();

      // FIXME bug 989: resume alarm service

      // resume all children
      if (log.isInfoEnabled()) {
        log.info("Recursively resuming all child components");
      }
      List childBinders = listBinders();
      for (int i = 0, n = childBinders.size(); i < n; i++) {
        Binder b = (Binder) childBinders.get(i);
        b.resume();
      }

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
  }


  public void stop() {
    super.stop();

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
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.stop();
    }

    // already transfered or released identity in "suspend()"

    if (log.isInfoEnabled()) {
      log.info("Stopped");
    }
  }

  public void halt() {
    // this seems reasonable:
    suspend();
    stop();
  }

  public void unload() {
    super.unload();

    if (log.isInfoEnabled()) {
      log.info("Unloading");
    }

    // unload in reverse order of "load()"

    ServiceBroker sb = getServiceBroker();

    // release child services
    sb.releaseService(this, BlackboardForAgent.class, myBlackboardService);

    // unload children
    if (log.isInfoEnabled()) {
      log.info("Recursively unloading all child components");
    }
    List childBinders = listBinders();
    for (int i = childBinders.size() - 1; i >= 0; i--) {
      Binder b = (Binder) childBinders.get(i);
      b.unload();
    }
    boundComponents.clear();

    //
    // release context-based services
    //

    if (log.isInfoEnabled()) {
      log.info("Releasing / revoking all services");
    }

    sb.releaseService(this, RealTimeService.class, rTimer);
    sb.releaseService(this, NaturalTimeService.class, xTimer);

    sb.revokeService(MessageSwitchService.class, myMessageSwitchSP);
    sb.revokeService(DemoControlService.class, myDemoControlServiceProvider);
    sb.revokeService(AlarmService.class, myAlarmServiceProvider);

    //
    // release remaining services
    //

    // messenger already released in "suspend()"

    // remove ourselves from the VM-local context
    if (log.isDebugEnabled()) {
      log.debug("Removing from the cluster context table");
    }
    ClusterContextTable.removeContext(getMessageAddress());

    // FIXME topology entry set to MOVING in "suspend()", need to
    // safely clear the entry without overwriting if the move was
    // successful.

    sb.releaseService(
        this, TopologyWriterService.class, myTopologyWriterService);
    sb.releaseService(
        this, TopologyReaderService.class, myTopologyReaderService);

    if (log.isInfoEnabled()) {
      log.info("Unloaded");
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = LoggingService.NULL;
    }

    sb.releaseService(
        this, AgentIdentityService.class, myAgentIdService);

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
   * Get the state of this cluster, which should be suspended.
   */
  public Object getState() {
    return getState(false);
  }

  /**
   * Kluge, for use by AgentManager's "cloneAgent(..)".
   */
  public Object getStateExcludingBlackboard() {
    return getState(true);
  }

  /**
   * Get the state of this cluster, which should be suspended.
   * <p>
   * Need to fix ContainerSupport for locking and hide
   * "boundComponents" access.
   */
  private Object getState(final boolean excludeBlackboard) {

    // get the child components
    List tuples;
    synchronized (boundComponents) {
      int n = boundComponents.size();
      tuples = new ArrayList(n);
      for (int i = 0; i < n; i++) {
        BoundComponent bc = (BoundComponent)
          boundComponents.get(i);
        Object comp = bc.getComponent();
        if (comp instanceof ComponentDescription) {
          ComponentDescription cd = (ComponentDescription) comp;
          Binder b = bc.getBinder();
          Object state;
          if (excludeBlackboard &&
              Blackboard.INSERTION_POINT.equals(
                cd.getInsertionPoint())) {
            state = null;
          } else {
            state = b.getState();
          }
          StateTuple ti = new StateTuple(cd, state);
          tuples.add(ti);
        } else {
          // error?
        }
      }
    }

    // get unsent messages
    List uMsgs = 
      ((unsentMessages != null) ?
       (unsentMessages) :
       (Collections.EMPTY_LIST));

    // get remote incarnations
    Object restartState = getRestartState();

    if (moveTargetNode != null) {
      // moving, get transferrable identity
      if (log.isInfoEnabled()) {
        log.info(
            "Transfering identity from node "+
            localNode+" to node "+moveTargetNode);
      }
      mobileIdentity = 
        myAgentIdService.transferTo(
            moveTargetNode);
      if (mobileIdentity == null) {
        mobileIdentity = NULL_MOBILE_IDENTITY;
      }
    } else {
      // non-moving state capture?
    }

    // create a state object
    AgentState result = 
      new AgentState(
          getMessageAddress(),
          incarnation,
          moveId,
          mobileIdentity,
          tuples,
	  mtsState,
          uMsgs,
          restartState);

    return result;
  }

  public void setState(Object loadState) {
    this.agentState = (AgentState) loadState;
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
            checkClusterInfo((MessageAddress) ((ClusterMessage) message).getSource());
          }

          if (showTraffic) showProgress("-");
          return false;         // don't ever consume it
        }
      });
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
      throw new UnsupportedOperationException( "Unsupported ComponentMessage: "+cm);
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

  /**
   * 
   **/
  private boolean updateIncarnation(MessageAddress agentId) {
    Long oldIncarnation = 
      (Long) restartIncarnationMap.get(agentId);
    if (oldIncarnation != null) {
      String agentName = agentId.getAddress();
      long newNumber = 
        myTopologyReaderService.getIncarnationForAgent(agentName);
      Long newIncarnation = new Long(newNumber);
      restartIncarnationMap.put(agentId, newIncarnation);
      long oldNumber = oldIncarnation.longValue();
      if (VERBOSE_RESTART && log.isDebugEnabled()) {
        log.debug(
            "Update agent "+agentId+
            " incarnation from "+oldIncarnation+
            " to "+newIncarnation);
      }
      return oldNumber != 0L && oldNumber != newNumber;
    }
    return false;
  }

  private Object getRestartState() {
    synchronized (restartIncarnationMap) {
      return new HashMap(restartIncarnationMap);
    }
  }

  private void setRestartState(Object o) {
    if (o != null) {
      needsRestart = false;
      Map m = (Map) o;
      synchronized (restartIncarnationMap) {
        restartIncarnationMap.putAll(m);
      }
    }
  }

  private void startRestartChecker() {
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

  private void stopRestartChecker() {
    if (restartTimer != null) {
      restartTimer.cancel();
      // note: If timer is running now then blackboard.restartAgent
      // may compain about the messenger being disabled.  This can be
      // ignored.
      restartTimer = null;
    }
  }

  /**
   * Called periodically to check for restarted agents. ClusterInfo
   * has an entry for every agent that we have communicated with. The
   * value is the last known incarnation number of the agent.
   *
   * The first time we check restarts, we have ourself restarted so we
   * proceed to verify our state against _all_ the other agents. We do
   * this because we have no record with whom we have been in
   * communication. In this case, we enumerate all the agents in the
   * nameserver and "restart" against all of them. Messages will be
   * sent only to those agents for which we have any directives
   * in common. The sending of those messages will
   * add entries to the restart incarnation map. So after doing a restart 
   * with an agent if there is an entry in the map for that agent, we set
   * the saved incarnation number to the current value for that agent.
   * This avoids repeating the restart later. If the current
   * incarnation number differs, the agent must have restarted so we
   * initiate the restart reconciliation process.
   *
   * On subsequent checks we only check agents listed in 
   * restartIncarnationMap.
   **/
  private void checkRestarts() {
    if (VERBOSE_RESTART && log.isDebugEnabled()) {
      log.debug("Check restarts");
    }
    List restartAgentIds = new ArrayList();
    synchronized (restartIncarnationMap) {
      try {
        for (Iterator i = restartIncarnationMap.keySet().iterator(); i.hasNext(); ) {
          MessageAddress agentId = (MessageAddress) i.next();
          if (updateIncarnation(agentId)) {
            restartAgentIds.add(agentId);
          }
        }
      } catch (Exception ne) {
        if (log.isInfoEnabled()) {
          log.info("Failed restart check", ne);
        }
      }
    }
    for (Iterator i = restartAgentIds.iterator(); i.hasNext(); ) {
      MessageAddress agentId = (MessageAddress) i.next();
      if (log.isInfoEnabled()) {
        log.info(
            "Detected (re)start of agent "+agentId+
            ", synchronizing blackboards");
      }
      // blackboard expects cluster-ids
      MessageAddress cid = (MessageAddress) agentId;
      myBlackboardService.restartAgent(cid);
    }
  }

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

  /**
   * Insure that we are tracking incarnations number for the agent at
   * a given address. If the specified agent is not in the restart
   * incarnation map it means we have never before communicated with that 
   * agent or we have just restarted and are sending restart messages. In 
   * both cases, it is ok to store the special "unknown incarnation" marker
   * because we do not want to detect any restart.
   **/
  private void checkClusterInfo(MessageAddress agentId) {
    if (VERBOSE_RESTART && log.isDebugEnabled()) {
      log.debug("Checking restart table for "+agentId);
    }
    // only include cluster-id message addresses in restart checking
    if (agentId instanceof MessageAddress) {
      synchronized (restartIncarnationMap) {
        if (restartIncarnationMap.get(agentId) == null) {
          if (VERBOSE_RESTART && log.isDebugEnabled()) {
            log.debug("Adding "+agentId+" to restart table");
          }
          restartIncarnationMap.put(agentId, new Long(0L));
        }
      }
    } else {
      if (VERBOSE_RESTART && log.isDebugEnabled()) {
        log.debug(
            "Ignoring message address "+
            ((agentId != null) ? agentId.getClass().getName() : "null"));
      }
    }
  }

  // required by the interface - ClusterMessage should really go away. Ugh.
  private void sendMessage(Message message)
  {
    checkClusterInfo(message.getTarget());
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
      System.out.print(p);
    }
  }

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

  // share our mts with the blackboard
  private static class IntraAgentMTSAdapter
    extends InternalAdapter
    implements IntraAgentMessageTransportService {
      public IntraAgentMTSAdapter(SimpleAgent sa) {
        super(sa);
      }
      public MessageAddress getMessageAddress() {
        return getAgent().getMessageAddress();
      }
      public void sendMessage(Message message) {
        getAgent().sendMessage(message);
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
        // switch to iamts?
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
      String msg = 
        "Unload of agent "+agent.getIdentifier()+
        " didn't result in garbage collection of the agent,"+
        " please use a profiler to check for a memory leak";
      // try to get the logger
      try {
        ServiceBroker sb = agent.getServiceBroker();
        LoggingService xlog = (LoggingService)
          sb.getService(
              agent, LoggingService.class, null);
        if ((xlog != null) && (xlog.isWarnEnabled())) {
          xlog.warn(msg);
        }
        sb.releaseService(agent, LoggingService.class, xlog);
        return;
      } catch (Exception e) {
      }
      // minimally print to standard-err
      System.err.println(msg);
    }
  }

  private static class AgentState implements java.io.Serializable {

    private final MessageAddress agentId;
    private final long incarnation;
    private final long moveId;
    private final TransferableIdentity identity;
    private final List tuples;  // List<StateTuple>
      private final org.cougaar.core.mts.AgentState mtsState;
    private final List unsentMessages; // List<ClusterMessage>
    private final Object restartState; // Map<MessageAddress><Long>

    public AgentState(
        MessageAddress agentId,
        long incarnation,
        long moveId,
        TransferableIdentity identity,
        List tuples,
	org.cougaar.core.mts.AgentState mtsState,
        List unsentMessages,
        Object restartState) {
      this.agentId = agentId;
      this.incarnation = incarnation;
      this.moveId = moveId;
      this.identity = identity;
      this.tuples = tuples;
      this.mtsState = mtsState;
      this.unsentMessages = unsentMessages;
      this.restartState = restartState;
      if ((agentId == null) ||
          (tuples == null) ||
          (unsentMessages == null)) {
        throw new IllegalArgumentException("null param");
      }
    }

    public String toString() {
      return 
        "Agent "+agentId+" state, incarnation "+incarnation+
        ", moveId "+moveId+", identity "+identity+
        ", tuples["+tuples.size()+
        "], mtsState "+(mtsState != null)+
	", unsentMessages["+unsentMessages.size()+
        "]";
    }

    private static final long serialVersionUID = 3109298128098682091L;
  }

  private class MessageTransportClientAdapter implements MessageTransportClient {
    public void receiveMessage(Message message) {
      SimpleAgent.this.receiveMessage(message);
    }

    public MessageAddress getMessageAddress() {
      return SimpleAgent.this.getMessageAddress();
    }
  }

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
