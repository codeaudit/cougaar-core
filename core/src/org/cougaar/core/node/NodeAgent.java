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

package org.cougaar.core.node;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.naming.NamingException;
import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.agent.AgentContainer;
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
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.node.service.RealTimeService;
import org.cougaar.core.persist.PersistenceClient;
import org.cougaar.core.persist.PersistenceIdentity;
import org.cougaar.core.persist.PersistenceServiceForAgent;
import org.cougaar.core.persist.RehydrationData;
import org.cougaar.core.plugin.PluginManager;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.NodeMetricsService;
import org.cougaar.util.CircularQueue;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * Implementation of an Agent which manages the resources and capabilities of a node.
 * <p>
 * Components and services are loaded in the following order:
 * <ul>
 * <li> <em>HIGH</em>: NodeControlService, LoggingService, external HIGH components, DefaultAgentIdentityComponent.
 * </li>
 * <li> <em>INTERNAL</em>: ThreadService, NamingService,
 * MetricsService, MetricsUpdateService, NodeMetricsService, MessageTransport,
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
 * @property org.cougaar.core.load.wp
 *   If enabled, the node will load the WhitePagesService
 *   component.  See bug 2522.  Default <em>true</em>
 *
 * @property org.cougaar.core.load.community
 *   If enabled, the node will load the CommunityService
 *   component.  See bug 2522.  Default <em>true</em>
 *
 * @property org.cougaar.core.load.planning
 *   If enabled, the node will load the planning-specific
 *   AssetInitializerService.  See bug 2522.
 *   Default <em>true</em>
 *
 * @property org.cougaar.core.load.servlet
 *   If enabled, the node will load the ServletService
 *   component.  See bug 2522.  Default <em>true</em>
 */
public class NodeAgent
  extends SimpleAgent
{
  private ServiceBroker agentServiceBroker = null;
  private AgentManager agentManager = null;
  private AgentContainer agentManagerProxy = null;

  private boolean ignoreRehydratedAgentDescs = false;
  private ComponentDescription[] agentDescs = null;

  private String nodeName = null;
  private MessageAddress nodeIdentifier = null;

  private List components = new ArrayList();

  private static final boolean isHeartbeatOn;
  private static final boolean isWPEnabled;
  private static final boolean isCommunityEnabled;
  private static final boolean isPlanningEnabled;
  private static final boolean isServletEnabled;

  private Logger logger = Logging.getLogger(NodeAgent.class);

  static {
    isHeartbeatOn=PropertyParser.getBoolean("org.cougaar.core.agent.heartbeat", true);
    isWPEnabled=PropertyParser.getBoolean("org.cougaar.core.load.wp", true);
    isCommunityEnabled=PropertyParser.getBoolean("org.cougaar.core.load.community", true);
    isPlanningEnabled=PropertyParser.getBoolean("org.cougaar.core.load.planning", true);
    isServletEnabled=PropertyParser.getBoolean("org.cougaar.core.load.servlet", true);
  }

  private class AgentManagerProxy implements AgentContainer {
    public boolean containsAgent(MessageAddress agentId) {
      return agentManager.containsAgent(agentId);
    }
    public Set getAgentAddresses() { return agentManager.getAgentAddresses(); }
    public ComponentDescription getAgentDescription(MessageAddress agentId) {
      return agentManager.getAgentDescription(agentId);
    }
    public Map getAgents() { return agentManager.getAgents(); }
    public void addAgent(MessageAddress agentId, StateTuple tuple) {
      agentManager.addAgent(agentId, tuple);
      persistNow();
    }
    public List getComponents() {
      return agentManager.getComponents();
    }
    public boolean add(Object o) {
      boolean result = agentManager.add(o);
      persistNow();
      return result;
    }
    public void removeAgent(MessageAddress agentId) {
      agentManager.removeAgent(agentId);
      persistNow();
    }
    public boolean remove(Object o) {
      boolean result = agentManager.remove(o);
      persistNow();
      return result;
    }
  }

  /**
   * Set the required NodeAgent parameter.
   *
   * @param o A list containing three elements, where the
   *    first element is the node address, and the
   *    second element is an unproxied reference to the top-level 
   *      ServiceBroker so that we can add global services, and the 
   *    third element is an unproxied reference to the AgentManager
   *      so that we can add agents.
   */
  public void setParameter(Object o) {
    List l = (List) o;
    nodeIdentifier = (MessageAddress) l.get(0);
    nodeName = nodeIdentifier.getAddress();
    super.setParameter(nodeIdentifier);
    agentServiceBroker = (ServiceBroker) l.get(1);
    agentManager = (AgentManager) l.get(2);
    agentManagerProxy = new AgentManagerProxy();
    ignoreRehydratedAgentDescs = ((Boolean) l.get(3)).booleanValue();
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
    ServiceBroker rootsb = agentServiceBroker;

    // set up the NodeControlService
    { 
      final Service _nodeControlService = new NodeControlService() {
        public ServiceBroker getRootServiceBroker() {
          return agentServiceBroker;
        }
        public AgentContainer getRootContainer() {
          return agentManagerProxy;
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
    add(new ComponentDescription(
          (getIdentifier()+"DefaultAgentIdentity"),
          Agent.INSERTION_POINT + ".Identity",
          "org.cougaar.core.node.DefaultAgentIdentityComponent",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null)); //policy
  }

  protected void loadInternalPriorityComponents() {
    ServiceBroker rootsb = agentServiceBroker;

    List threadServiceParams = new ArrayList();
    threadServiceParams.add("name=Node " + nodeName);
    threadServiceParams.add("isRoot=true"); // hack to use rootsb
    add(new ComponentDescription(
          (getIdentifier()+"Threads"),
          Agent.INSERTION_POINT + ".Threads",
          "org.cougaar.core.thread.ThreadServiceProvider",
          null,  //codebase
          threadServiceParams,  //parameters
          null,  //certificate
          null,  //lease
          null)); //policy

    try {
      rootsb.addService(NamingService.class,
          new NamingServiceProvider(
            SystemProperties.getSystemPropertiesWithPrefix("java.naming.")));
    } catch (NamingException ne) {
      throw new Error("Couldn't initialize NamingService ", ne);
    }

    if (isWPEnabled) {
      add(new ComponentDescription(
            (getIdentifier()+"WhitePages"),
            Agent.INSERTION_POINT + ".WP",
            "org.cougaar.core.naming.JNDIWhitePagesServiceComponent",
            null,  //codebase
            null,  //parameters
            null,  //certificate
            null,  //lease
            null)); //policy
    }

    add(new ComponentDescription(
          (getIdentifier()+"MetricsServices"),
          Agent.INSERTION_POINT + ".MetricsServices",
          "org.cougaar.core.qos.metrics.MetricsServiceProvider",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null)); //policy

    //add the vm metrics
    rootsb.addService(NodeMetricsService.class,
        new NodeMetricsServiceProvider(new NodeMetricsProxy()));

    // Load agents from either the rehydrated agents map or from the
    // ComponentInitializerService
    if (ignoreRehydratedAgentDescs && agentDescs != null) {
      if (logger.isInfoEnabled()) {
        logger.info("Ignoring rehydrated list of " + agentDescs.length + " agents");
      }
      agentDescs = null;
    }
    if (agentDescs == null) {
      try {
        ComponentInitializerService cis = (ComponentInitializerService) 
          rootsb.getService(this, ComponentInitializerService.class, null);
        if (logger.isInfoEnabled())
          logger.info("NodeAgent(" + nodeName + ").loadInternal about to ask for agents");
        // get the agents - this gives _anything_ below AgentManager,
        // so must extract out just the .Agent's later (done in addAgents)
        agentDescs =
          cis.getComponentDescriptions(nodeName, AgentManager.INSERTION_POINT);
        rootsb.releaseService(this, ComponentInitializerService.class, cis);
        if (logger.isInfoEnabled()) {
          logger.info("Using ComponentInitializerService list of " + agentDescs.length + " agents");
        }
      } catch (Exception e) {
        throw new Error("Couldn't initialize NodeAgent from ComponentInitializerService ", e);
      }
    }

    // Set up MTS service provides.
    //
    // NB: The order is important for now - MTS *must* be created
    // first.
    add(new ComponentDescription(
          (getIdentifier()+"MessageTransport"),
          Agent.INSERTION_POINT + ".MessageTransport",
          "org.cougaar.core.mts.MessageTransportServiceProvider",
          null,  //codebase
          null,  //parameters
          null,  //certificate
          null,  //lease
          null)); //policy

    {
      TimeServiceProvider tsp = new TimeServiceProvider();
      tsp.start();
      rootsb.addService(NaturalTimeService.class, tsp);
      rootsb.addService(RealTimeService.class, tsp);
    }

    if (isServletEnabled) {
      // start up the Node-level ServletService component
      add(new ComponentDescription(
            (getIdentifier()+"ServletService"),
            Agent.INSERTION_POINT + ".NodeServletService",
            "org.cougaar.lib.web.service.RootServletServiceComponent",
            null,  //codebase
            null,  //parameters
            null,  //certificate
            null,  //lease
            null)); //policy
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
    if (isCommunityEnabled) {
      // If no other CommunityInitService available, and we're using communities,
      // then use the default InitServiceComponent -- CSMART DB
      // if components loaded from there, otherwise Files XML)
      // To provide your own (ie, non-CSMART DB) InitService, load a component
      // at BINDER priority
      add(new ComponentDescription(
            "community-init",
            Agent.INSERTION_POINT+".Init",
            "org.cougaar.community.init.CommunityInitializerServiceComponent",
            null,  //codebase
            null,  //params
            null,  //certificate
            null,  //lease
            null,  //policy
            ComponentDescription.PRIORITY_COMPONENT));
    }

    if (isPlanningEnabled) {
      // If no other AssetInitService loaded, load the default
      // InitServiceComponent: CSMART DB Initialization
      // if Components loaded from there, Non-CSMART DB if Component 
      // initialized from XML, otherwise INI intializations.
      // If you want your own AssetInitService 
      // specify your own component at BINDER priority
      add(new ComponentDescription(
            "asset-init",
            Agent.INSERTION_POINT+".Init",
            "org.cougaar.planning.ldm.AssetInitializerServiceComponent",
            null,  //codebase
            null,  //params
            null,  //certificate
            null,  //lease
            null,  //policy
            ComponentDescription.PRIORITY_COMPONENT));
    }

    super.loadComponentPriorityComponents();
  }

  protected void loadLowPriorityComponents() {
    super.loadLowPriorityComponents();
  }

  private void persistNow() {
    if (getModelState() == ACTIVE) {
      myBlackboardService.persistNow();
      if (logger.isInfoEnabled()) logger.info("persistNow()");
    }
  }

  protected PersistenceClient getPersistenceClient() {
    final PersistenceClient superPersistenceClient = super.getPersistenceClient();
    return new PersistenceClient() {
        public PersistenceIdentity getPersistenceIdentity() {
          return superPersistenceClient.getPersistenceIdentity();
        }
        public List getPersistenceData() {
          List superData = superPersistenceClient.getPersistenceData();
          List data = new ArrayList(superData.size() + 1);
          data.addAll(superData);
          List components = agentManager.getComponents();
          // Find and remove ourself from the agent collection
          for (Iterator i = components.iterator(); i.hasNext(); ) {
            ComponentDescription cd = (ComponentDescription) i.next();
            if (NodeAgent.class.getName().equals(cd.getClassname())) {
              if (logger.isDebugEnabled()) logger.debug("Removing " + cd);
              i.remove();
              break;
            } else {
              if (logger.isDebugEnabled()) logger.debug("Keeping " + cd);
            }
          }
          data.add(components.toArray(new ComponentDescription[components.size()]));
          return data;
        }
      };
  }

  protected List getRehydrationList(PersistenceServiceForAgent persistenceService) {
    RehydrationData rd = persistenceService.getRehydrationData();
    if (rd != null) {
      List rehydrationList = rd.getObjects();
      int len = rehydrationList.size();
      if (len > 0) {
        agentDescs = (ComponentDescription[]) rehydrationList.get(len - 1);
        rehydrationList.remove(len - 1);
      }
      return rehydrationList;
    }
    return null;
  }

  public void load() 
  {
    super.load();

    // Wait until the end to deal with outstanding queued messages
    emptyQueuedMessages();

    // load the agents
    addAgents(agentDescs);
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
    if (logger.isDebugEnabled())
      logger.debug(nodeName + ":AgentManager.addAgents got list of length " + descs.length);
    ComponentDescriptions cds = new ComponentDescriptions(descs);
    List cdcs = cds.extractInsertionPointComponent(Agent.INSERTION_POINT);
    //logger.debug(nodeName + ":AgentManager.addAgents after extraction by ins pt have " + cdcs.size());
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
          if (!(Agent.INSERTION_POINT.equals(ip))) {
            throw new UnsupportedOperationException(
                "Only Agent ADD supported for now, not "+ip);
          }
          agentManager.add(st);
        } else {
          // not implemented yet!  will be okay once Node is a Container
          throw new UnsupportedOperationException(
              "Unsupported ComponentMessage: "+m);
        }
      } else if (m.getTarget().equals(MessageAddress.MULTICAST_SOCIETY)) {
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
  private static class NodeAgentBinderFactory extends BinderFactorySupport {
    // bind everything but NodeAgent's PluginManager
    public Binder getBinder(Object child) {
      if (! (child instanceof PluginManager)) {
        return new NodeAgentBinder(this, child);
      } else {
        return null;
      }
    }
    private static class NodeAgentBinder 
      extends BinderSupport
      implements BindingSite {
        public NodeAgentBinder(BinderFactory bf, Object child) {
          super(bf, child);
        }
        protected BindingSite getBinderProxy() {
          return this;
        }
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
