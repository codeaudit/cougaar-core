/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentBinder;
import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.SimpleAgent;
import org.cougaar.core.component.Binder;
import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderFactorySupport;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.Container;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.mobility.service.MobilityMessage;
import org.cougaar.core.mobility.service.RootMobilityComponent;
import org.cougaar.core.mts.AgentStatusService;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportServiceProvider;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.node.InitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.PluginManager;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsServiceProvider;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageWatcherService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.NodeMetricsService;
import org.cougaar.core.thread.ThreadServiceProvider;
import org.cougaar.util.log.*;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.CircularQueue;

import org.cougaar.core.node.service.*;

/**
 * Implementation of an Agent which manages the resources and capabilities of a node.
 * <p>
 * Components and services are loaded in the following order:
 * <ul>
 * <li> <em>HIGH</em>: NodeControlService, LoggingService, external HIGH components, DefaultAgentIdentityComponent.
 * </li>
 * <li> <em>INTERNAL</em>: ThreadService, NamingService, TopologyWriterServiceComponent, TopologyReaderServiceComponent,
 * MetricsService, MetricsUpdateService, NodeMetricsService, MessageTransport, NodeTrustComponent, RootMobilityComponent,
 * RootServletComponent, external INTERNAL components.
 * </li>
 * <li> <em>BINDER</em>: NodeAgentBinderFactory, external BINDER components.
 * </li>
 * <li> <em>COMPONENT</em>: external COMPONENT components.
 * </li>
 * <li> <em>LOW</em>: external LOW components.
 * </li>
 * </ul>
 * @property org.cougaar.core.agent.heartbeat
 *   If enabled, a low-priority thread runs and prints
 *   a '.' every few seconds when nothing else much is going on.
 *   This is a one-per-vm function.  Default <em>true</em>.
 *
 * @property org.cougaar.core.agent quiet
 *   Makes standard output as quiet as possible.
 */
public class NodeAgent
  extends SimpleAgent
{
  private ServiceBroker agentServiceBroker = null;
  private AgentManager agentManager = null;

  private ComponentDescription[] agentDescs = null;

  /** A reference to the MessageTransportService containing the Messenger **/
  private transient MessageTransportService theMessenger = null;

  private RootMobilityComponent agentMobility;

  private String nodeName = null;
  private NodeIdentifier nodeIdentifier = null;

  private static boolean isHeartbeatOn = true;
  private static boolean isQuiet = false;

  private Logger logger = Logging.getLogger(NodeAgent.class);

  static {
    isHeartbeatOn=PropertyParser.getBoolean("org.cougaar.core.agent.heartbeat", true);
    isQuiet=PropertyParser.getBoolean("org.cougaar.core.agent.quiet", false);
  }


  /**
   * Set the required NodeAgent parameter.
   *
   * @param o A list containing two elements, where the first element is
   *    an unproxied reference to the top-level ServiceBroker so that 
   *    we can add global services, and the second element is
   *    an unproxied reference to the AgentManager so that we can 
   *    add agents.
   */
  public void setParameter(Object o) {
    List l = (List) o;
    agentServiceBroker = (ServiceBroker) l.get(0);
    agentManager = (AgentManager) l.get(1);
  }


  /** Activates heartbeat (if enabled) and then continues to start the node.
   **/
  public void start() {
    if (isHeartbeatOn) {
      startHeartbeat();
    }
    super.start();
  }

  ///
  /// subcomponent phases
  /// 

  protected void loadHighPriorityComponents() {
    ServiceBroker localsb = getServiceBroker();
    ServiceBroker rootsb = agentServiceBroker;
    AgentManager am = agentManager;

    // set up the NodeControlService
    { 
      final Service _nodeControlService = new NodeControlService() {
        public ServiceBroker getRootServiceBroker() {
          return agentServiceBroker;
        }
        public Container getRootContainer() {
          return agentManager;
        }
      };

      ServiceProvider ncsp = new ServiceProvider() {
        public Object getService(ServiceBroker xsb, Object requestor, Class serviceClass) {
          if (serviceClass == NodeControlService.class) {
            return _nodeControlService;
          } else {
            throw new IllegalArgumentException("Can only provide NodeControlService!");
          }
        }
        public void releaseService(
            ServiceBroker xsb, Object requestor, Class serviceClass, Object service) {
        }
      };
      getServiceBroker().addService(NodeControlService.class, ncsp);
    }

    {
      LoggingServiceProvider lsp = new LoggingServiceProvider();
      rootsb.addService(LoggingService.class, lsp);
      rootsb.addService(LoggingControlService.class, lsp);
    }

    super.loadHighPriorityComponents();

    // add the default agent-identity provider, which does
    // nothing if a high-priority id provider already exists
    ComponentDescription defaultAgentIdCDesc = 
      new ComponentDescription(
          (getIdentifier()+"DefaultAgentIdentity"),
          "Node.AgentManager.Agent.Identity",
          "org.cougaar.core.node.DefaultAgentIdentityComponent",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null); //policy
    add(defaultAgentIdCDesc);
  }

  protected void loadInternalPriorityComponents() {
    ServiceBroker localsb = getServiceBroker();
    ServiceBroker rootsb = agentServiceBroker;
    AgentManager am = agentManager;

    {
      ThreadServiceProvider tsp = new ThreadServiceProvider(rootsb, "Node " + nodeName);
      tsp.provideServices(rootsb);
    }

    try {
      rootsb.addService(NamingService.class,
          new NamingServiceProvider(
            SystemProperties.getSystemPropertiesWithPrefix("java.naming.")));
    } catch (NamingException ne) {
      throw new Error("Couldn't initialize NamingService ", ne);
    }

    ComponentDescription topologyWriterSCDesc = 
      new ComponentDescription(
          (getIdentifier()+"TopologyWriter"),
          "Node.AgentManager.Agent.Topology",
          "org.cougaar.core.topology.TopologyWriterServiceComponent",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null); //policy
    add(topologyWriterSCDesc);

    ComponentDescription topologyReaderSCDesc = 
      new ComponentDescription(
          (getIdentifier()+"TopologyReader"),
          "Node.AgentManager.Agent.Topology",
          "org.cougaar.core.topology.TopologyReaderServiceComponent",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null); //policy
    add(topologyReaderSCDesc);

    MetricsServiceProvider msp = new MetricsServiceProvider(rootsb, nodeIdentifier);
    rootsb.addService(MetricsService.class, msp);
    rootsb.addService(MetricsUpdateService.class, msp);


    //add the vm metrics
    rootsb.addService(NodeMetricsService.class,
        new NodeMetricsServiceProvider(new NodeMetricsProxy()));

    try {
      InitializerService is = (InitializerService) 
        rootsb.getService(this, InitializerService.class, null);
      agentDescs =
        is.getComponentDescriptions(nodeName, "Node.AgentManager");
      rootsb.releaseService(this, InitializerService.class, is);
    } catch (Exception e) {
      throw new Error("Couldn't initialize NodeAgent from InitializerService ", e);
    }


    // Set up MTS service provides.
    //
    // NB: The order is important for now - MTS *must* be created
    // first.
    initTransport(rootsb, nodeIdentifier);  

    // register for external control by the AppServer
    //   -- disabled for now --

    // start up the NodeTrust component
    String ntc = new String(getIdentifier()+"NodeTrust");
    ComponentDescription ntcdesc = 
      new ComponentDescription(
          ntc,
          "Node.AgentManager.Agent.NodeTrust",
          "org.cougaar.core.node.NodeTrustComponent",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null); //policy
    add(ntcdesc);         // let a ComponentLoadFailure pass through

    // node-level agent mobility service provider
    List mobilityParams = new ArrayList(2);
    mobilityParams.add(theMessenger);
    mobilityParams.add(agentManager);
    // FIXME would use desc-based "add", but we need a pointer
    // to the component for later message-passing.
    //
    // for now we'll directly add the component
    this.agentMobility = 
      new RootMobilityComponent(
          mobilityParams);
    add(agentMobility);
    agentMobility.provideServices(rootsb);

    String enableServlets = 
      System.getProperty("org.cougaar.core.servlet.enable");
    if ((enableServlets == null) ||
        (enableServlets.equalsIgnoreCase("true"))) {
      // start up the Node-level ServletService component
      ComponentDescription nsscDesc = 
        new ComponentDescription(
            (getIdentifier()+"ServletService"),
            "Node.AgentManager.Agent.NodeServletService",
            "org.cougaar.lib.web.service.RootServletServiceComponent",
            null,  //codebase
            null,  //parameters
            null,  //certificate
            null,  //lease
            null); //policy
      try {
        add(nsscDesc);
      } catch (RuntimeException re) {
        re.printStackTrace();
      }
    }

    {
      TimeServiceProvider tsp = new TimeServiceProvider();
      tsp.start();
      rootsb.addService(NaturalTimeService.class, tsp);
      rootsb.addService(RealTimeService.class, tsp);
    }

    super.loadInternalPriorityComponents();

  }

  protected void loadBinderPriorityComponents() {
    // set up our binder factory
    {
      BinderFactory nabf = new NodeAgentBinderFactory();
      if (!attachBinderFactory(nabf)) {
        throw new Error("Failed to load the NodeAgentBinderFactory in NodeAgent");
      }
    }

    super.loadBinderPriorityComponents();
  }

  protected void loadComponentPriorityComponents() {
    super.loadComponentPriorityComponents();

  }
  protected void loadLowPriorityComponents() {
    super.loadLowPriorityComponents();
  }

  public void load() 
  {
    ServiceBroker localsb = getServiceBroker();
    ServiceBroker rootsb = agentServiceBroker;
    AgentManager am = agentManager;

    try {
      NodeIdentificationService nis = (NodeIdentificationService) 
        rootsb.getService(this,NodeIdentificationService.class,null);
      if (nis != null) {
        nodeIdentifier = nis.getNodeIdentifier();
        nodeName = nodeIdentifier.toString();
      } else {
        throw new RuntimeException("No node name specified");
      }
    } catch (RuntimeException e) {
      throw new Error("Couldn't figure out Node name ", e);
    }

    // set the MessageAddress to be a cid for now (sigh)
    setMessageAddress( new ClusterIdentifier(nodeName) );

    String filename = System.getProperty("org.cougaar.filename");
    String experimentId = System.getProperty("org.cougaar.experiment.id");
    if (filename == null) {
      if (experimentId == null) {
        // use the default "name.ini"
        filename = nodeName + ".ini";
      } else {
        // use the filename
      }
    } else if (experimentId == null) {
      // use the experimentId
    } else {
      throw new IllegalArgumentException(
          "Both file name (-f) and experiment -X) specified. "+
          "Only one allowed.");
    }


    try {
      ServiceProvider sp;
      if (filename != null) {
        sp = new FileInitializerServiceProvider();
      } else {
        sp = new DBInitializerServiceProvider(experimentId);
      }
      rootsb.addService(InitializerService.class, sp);
    } catch (Exception yech) {
      throw new Error("Couldn't initialize node", yech);
    }

    //mgmtLP = new MgmtLP(this); // MTMTMT turn off till RMI namespace works

    super.load();

    // Wait until the end to deal with outstanding queued messages
    emptyQueuedMessages();

    // load the clusters
    //
    // once bulk-add ComponentMessages are implements this can
    //   be done with "this.receiveMessage(compMsg)"
    addAgents(agentDescs);
  }

  // replace with Container's add, but keep this basic code
  protected void addAgent(ComponentDescription desc) {
    // simply wrap as a single-element "bulk" operation
    ComponentDescription[] descs = new ComponentDescription[1];
    descs[0] = desc;
    addAgents(descs);
  }

  /**
   * Add Agents and their child Components (Plugins, etc) to this Node.
   * <p>
   * Note that this is a bulk operation, since the loading process is:<ol>
   *   <li>Create the empty clusters</li>
   *   <li>Add the Plugins and initialize the clusters</li>
   * </ol>
   * <p>
   */
  protected void addAgents(ComponentDescription[] descs) {
    ComponentDescriptions cds = new ComponentDescriptions(descs);
    List cdcs = cds.extractDirectComponents("Node.AgentManager");
    try {
      agentManager.addAll(cdcs);
    } catch (RuntimeException re) {
      re.printStackTrace();
    }
  }

  private class NodeMetricsProxy implements NodeMetricsService {
    /** Free Memory snapshot from the Java VM   **/
    public long getFreeMemory() {
      return Runtime.getRuntime().freeMemory();
    }
    /** Total memory snaphsot from the Java VM    */
    public long getTotalMemory() {
      return Runtime.getRuntime().totalMemory();
    }
    /** The number of active Threads in the main COUGAAR threadgroup **/
    public int getActiveThreadCount() {
      return Thread.currentThread().getThreadGroup().activeCount();
    }
  }

  // **** QUO *****
  // Change this to create (or find?) a MessageTransportManager as the
  // value of theMessenger.
  private void initTransport(ServiceBroker rootsb, NodeIdentifier id) {
    String name = id.toString();
    MessageTransportServiceProvider mtsp = 
      new MessageTransportServiceProvider(name);
    add(mtsp);

    rootsb.addService(MessageTransportService.class, mtsp);
    rootsb.addService(MessageStatisticsService.class, mtsp);
    rootsb.addService(MessageWatcherService.class, mtsp);
    rootsb.addService(AgentStatusService.class, mtsp);

    theMessenger = (MessageTransportService)
      getServiceBroker().getService(this, MessageTransportService.class, null);
    //System.err.println("Started "+theMessenger);
    theMessenger.registerClient(this);

  }


  private class MTSClient implements MessageTransportClient {
    public void receiveMessage(Message message) {
      NodeAgent.this.receiveMessage(message);
    }

    public MessageAddress getMessageAddress() {
      return NodeAgent.this.getMessageAddress();
    }
  }

  /** deliver or queue the message.  
   * We'll queue the message until we're really completely up
   * and running.
   **/
  public void receiveMessage(Message m) {
    synchronized (mq) {
      if (mqInitialized) {      // fully emptied?
        realReceiveMessage(m);  // don't bother to queue it after all
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("Queuing NodeAgent Message "+m);
        }
        mq.add(m);
        // mq.notify();// skip since there isn't anyone listening
      }
    }
  }

  /** Queue for messages waiting while we're still starting up. **/
  private final CircularQueue mq = new CircularQueue();

  /** true once we've dealt with any early messages **/
  private boolean mqInitialized = false;

  /** Deal with any queued messages and then allow receiveMessage to
   * start delivering directly.
   **/
  private void emptyQueuedMessages() {
    synchronized (mq) {
      if (!mq.isEmpty()) {
        if (logger.isInfoEnabled()) {
          logger.info("Delivering "+mq.size()+" queued NodeAgent Messages");
        }
        while (!mq.isEmpty()) {
          Message m = (Message) mq.next();
          if (logger.isDebugEnabled()) {
            logger.debug("Delivering queued NodeAgent Message "+m);
          }
          realReceiveMessage(m);
        }
      }
      mqInitialized = true;
    }
  }

  /** really deliver the message.
   * A better solution would be to just use the SimpleAgent's 
   * message queue and deal with the messages there, but there
   * are some conflicts and missing bits of information up there.  
   * In particular, the mobility reference isn't available.
   **/
  private void realReceiveMessage(final Message m) {
    try {
      if (m instanceof ComponentMessage) {
        ComponentMessage cm = (ComponentMessage)m;
        int operation = cm.getOperation();
        if (operation == ComponentMessage.ADD) {
          // add
          ComponentDescription cd = cm.getComponentDescription();
          StateTuple st = 
            new StateTuple(
                cd,
                cm.getState());
          // should do "add(st)", but requires Node fixes
          //
          // for now we do this work-around:
          String ip = cd.getInsertionPoint();
          if (!("Node.AgentManager.Agent".equals(ip))) {
            throw new UnsupportedOperationException(
                "Only Agent ADD supported for now, not "+ip);
          }
          agentManager.add(st);
        } else {
          // not implemented yet!  will be okay once Node is a Container
          throw new UnsupportedOperationException(
              "Unsupported ComponentMessage: "+m);
        }
      } else if (m instanceof MobilityMessage) {
        if (agentMobility != null) {
          agentMobility.receiveMessage(m);
        } else {
          throw new RuntimeException(
              "Agent mobility disabled in node "+getIdentifier());
        }
      } else if (m.getTarget().equals(MessageAddress.SOCIETY)) {
        // we don't do anything with these. ignore it.
      } else {
        super.receiveMessage(m);
      }
    } catch (Exception e) {
      getLogger().warn("NodeAgent "+this+" received invalid message: "+m, e);
    }
  }

  //
  // Binder for children
  //
  private class NodeAgentBinderFactory extends BinderFactorySupport {
    // bind everything but NodeAgent's PluginManager
    public Binder getBinder(Object child) {
      if (! (child instanceof PluginManager)) {
        return new NodeAgentBinder(this, child);
      } else {
        return null;
      }
    }
  }

  private class NodeAgentBinder 
    extends BinderSupport
    implements NodeAgentBindingSite, AgentBinder
    {
      public NodeAgentBinder(BinderFactory bf, Object child) {
        super(bf, child);
      }
      protected BindingSite getBinderProxy() {
        return this;
      }
      public MessageAddress getAgentIdentifier() {
        return (getAgent()).getAgentIdentifier();
      }
      public Agent getAgent() {
        return NodeAgent.this;
      }
    }

  //
  // heartbeat
  //
  private Heartbeat heartbeat = null;
  
  private void startHeartbeat() {
    heartbeat = new Heartbeat();
    heartbeat.start();
  }

}
