/*
 * <copyright>
 *  
 *  Copyright 2001-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cougaar.bootstrap.SystemProperties;
import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.ComponentDescriptions;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.ComponentInitializerService;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.util.Memo;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/**
 * A container for Agents.
 * <p> 
 * Although the AgentManager can hold Components other than Agents,
 * the default BinderFactory will only actually accept Agents and
 * other Binders.  If you want to load other sorts of components
 * into AgentManager, you'll need to supply a Binder which knows
 * how to bind your Component class.
 *
 * @property org.cougaar.core.node.InitializationComponent
 * Which component should be loaded to advertise the
 * ComponentInitializerService, which provides the agent
 * component description configurations.  Usually set to XML.
 */
public class AgentManager
    extends ContainerSupport
    implements AgentContainer {
  public static final String INSERTION_POINT = "Node.AgentManager";

  private static final String FILENAME_PROP = "org.cougaar.filename";
  private static final String EXPTID_PROP = "org.cougaar.experiment.id";
  public static final String INITIALIZER_PROP =
      "org.cougaar.core.node.InitializationComponent";
  private static final String NODE_AGENT_CLASSNAME_PROPERTY =
      "org.cougaar.core.node.classname";

  private ServiceProvider nodeIdentificationSP;
  private ServiceProvider nodeControlSP;
  
  private boolean isNodeShuttingDown = false;
  private final Object shutdownLock = new Object();
  
  public void load() {
    super.load();

    String nodeName = getNodeName();

    add_node_identification_service(nodeName);

    add_node_control_service(nodeName);

    add(getInitializerDescription());

    addAll(getAgentBinderDescriptions(nodeName));

    add(getNodeAgentDescription(nodeName));
  }

  /**
   * Unload Agent manager.
   * <p>
   * 
   * @see org.cougaar.util.GenericStateModel#unload()
   */
  public void unload() {
    revokeServices();
    super.unload();
  }
  
  private void revokeServices() {
    if (nodeIdentificationSP != null) {
      getServiceBroker().revokeService(
          NodeIdentificationService.class, nodeIdentificationSP);
      nodeIdentificationSP = null;
    }
    if (nodeControlSP != null) {
      getServiceBroker().revokeService(
          NodeControlService.class, nodeControlSP);
      nodeControlSP = null;
    }
  }

  protected String specifyContainmentPoint() {
    return INSERTION_POINT;
  }

  protected ComponentDescriptions findInitialComponentDescriptions() {
    return null;
  }

  //
  // override "add(o)" to log exceptions:
  //

  public boolean add(Object o) {
    try {
      return super.add(o);
    } catch (RuntimeException re) {
      Logging.getLogger(this.getClass()).error("Failed to add " + o + " to " + this, re);
      return false;
    }
  }

  //
  // methods used by "load()":
  //

  private String getNodeName() {
    String nodeName = SystemProperties.getProperty("org.cougaar.node.name");
    if (nodeName == null) {
      try {
        nodeName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException uhe) {
        throw new RuntimeException("Node name not specified and couldn't guess from local host name.", uhe);
      }
      if (nodeName == null) {
        throw new IllegalArgumentException("Node name not specified");
      }
    }
    return nodeName;
  }

  private void add_node_identification_service(String nodeName) {
    final MessageAddress localNode =
        MessageAddress.getMessageAddress(nodeName);
    Class clazz = NodeIdentificationService.class;
    Service service =
        new NodeIdentificationService() {
          public MessageAddress getMessageAddress() {
            return localNode;
          }
        };
    nodeIdentificationSP = add_service(clazz, service);
  }

  private void add_node_control_service(final String nodeName) {
    // instead of using the node's or our service broker as the
    // "rootsb", we use our child service broker.  All components
    // and agents are loaded as our children, so this is should
    // be fine.
    final ServiceBroker rootsb = getChildServiceBroker();
    // create a proxy for our agent manager, to prevent casting
    final AgentContainer rootac =
        new AgentContainer() {
          public boolean containsAgent(MessageAddress agentId) {
            return AgentManager.this.containsAgent(agentId);
          }

          public Set getAgentAddresses() {
            return AgentManager.this.getAgentAddresses();
          }

          public ComponentDescription getAgentDescription(MessageAddress agentId) {
            return AgentManager.this.getAgentDescription(agentId);
          }

          public Map getAgents() {
            return AgentManager.this.getAgents();
          }

          public List getComponents() {
            return AgentManager.this.getComponents();
          }

          public void addAgent(MessageAddress agentId, StateTuple tuple) {
            AgentManager.this.addAgent(agentId, tuple);
          }

          public boolean add(Object o) {
            return AgentManager.this.add(o);
          }

          public void removeAgent(MessageAddress agentId) {
            AgentManager.this.removeAgent(agentId);
          }

          public boolean remove(Object o) {
            return AgentManager.this.remove(o);
          }
        };
    Class clazz = NodeControlService.class;
    Service service =
        new NodeControlService() {
          public ServiceBroker getRootServiceBroker() {
            return rootsb;
          }

          public AgentContainer getRootContainer() {
            return rootac;
          }

          public void shutdown() {
            final MessageAddress localNode =
              MessageAddress.getMessageAddress(nodeName);
            shutdownNode(localNode);
          }
        };
    nodeControlSP = add_service(clazz, service);
  }

  private ComponentDescription getInitializerDescription() {
    // we need the ComponentInitializerService for to read the
    // agent binder descriptions in "getAgentBinderDescriptions",
    // so we must load it here.
    //
    // Ideally we'd do this within each agent, but that's not
    // possible if we must support agent binders.
    String classname = getComponentInitializerClass();
    return
        new ComponentDescription(classname,
            AgentManager.INSERTION_POINT + ".Component",
            classname,
            null, //codebase
            null, //params
            null, //certificate
            null, //lease
            null, //policy
            ComponentDescription.PRIORITY_COMPONENT);
  }

  private List getAgentBinderDescriptions(String nodeName) {
    ServiceBroker csb = getChildServiceBroker();
    ComponentDescriptions cds;
    try {
      ComponentInitializerService cis = (ComponentInitializerService)
          csb.getService(this, ComponentInitializerService.class, null);
      Logger logger = Logging.getLogger(AgentManager.class);
      if (logger.isInfoEnabled()) {
        logger.info(nodeName +
            " AgentManager.load about to look for CompDesc's" +
            " of Agent Binders.");
      }
      // Get all items _below_ given insertion point.
      // To get just binders, must use extract method later....
      cds = new ComponentDescriptions(cis.getComponentDescriptions(nodeName, INSERTION_POINT));
      csb.releaseService(this, ComponentInitializerService.class, cis);
    } catch (Exception e) {
      throw new Error("Couldn't initialize AgentManager Binders" +
          "  with ComponentInitializerService ", e);
    }

    return
        ComponentDescriptions.sort(cds.extractInsertionPointComponent(INSERTION_POINT + ".Binder"));
  }

  private ComponentDescription getNodeAgentDescription(String nodeName) {
    String classname = SystemProperties.getProperty(NODE_AGENT_CLASSNAME_PROPERTY,
        "org.cougaar.core.agent.AgentImpl");
    List params = new ArrayList(1);
    params.add(nodeName);
    ComponentDescription desc =
        new ComponentDescription(classname,
            Agent.INSERTION_POINT,
            classname,
            null, //codebase
            params,
            null, //certificate
            null, //lease
            null, //policy
            ComponentDescription.PRIORITY_COMPONENT);
    return desc;
  }

  private String getComponentInitializerClass() {
    String component = SystemProperties.getProperty(INITIALIZER_PROP);
    if (component == null) {
      component = getDefaultComponentInitializerClass();
      SystemProperties.setProperty(INITIALIZER_PROP, component);
    }
    // if full class name not specified, intuit it
    if (component.indexOf(".") < 0) {
      // build up the name, full name was not specified.
      component =
          "org.cougaar.core.node." +
          component +
          "ComponentInitializerServiceComponent";
    }
    Logger logger = Logging.getLogger(AgentManager.class);
    if (logger.isInfoEnabled()) {
      logger.info("Will intialize components from " + component);
    }
    return component;
  }

  private String getDefaultComponentInitializerClass() {
    // figure out whether to use Files or CSMART DB for component
    // initalization (since it's not explicitly specified by
    // a -D argument)
    //
    // This will use the CSMART DB (advertising the DBInitializerService)
    // if the experiment_id system property was set, and some other
    // initializer was not selected.  Otherwise, INI files are used
    // (and no DBInitializerService is provided) however, users may
    // specify, for example, an XML initializer
    String filename = SystemProperties.getProperty(FILENAME_PROP);
    String expt = SystemProperties.getProperty(EXPTID_PROP);
    Logger logger = Logging.getLogger(AgentManager.class);
    if ((filename == null) && (expt == null)) {
      // use the default "name.ini"
      if (logger.isWarnEnabled()) {
        logger.warn("Defaulting to DEPRECATED INI File based Initialization.");
        logger.warn("Got no filename or experimentId! Using default File");
      }
      return "File";
    }
    if (filename == null) {
      // use the experiment ID to read from the DB
      if (logger.isWarnEnabled()) {
        logger.warn("Got no filename, using exptID " + expt);
      }
      return "DB";
    }
    if (expt == null) {
      // use the filename provided
      if (logger.isWarnEnabled()) {
        logger.warn("Defaulting to DEPRECATED INI File based Initialization.");
        logger.warn("Got no exptID, using given filename " + filename);
      }
    }
    return "File";
  }

  //
  // Implement the "AgentContainer" API, primarily to support
  //   agent mobility
  //

  public boolean containsAgent(MessageAddress agentId) {
    return (getAgentDescription(agentId) != null);
  }

  private Memo _getAgentAddressesMemo = Memo.get(new Memo.Function() {
      public String toString() { return "Memo<getAgentAddresses>"; }
      public Object eval(Object o) {
        return Collections.unmodifiableSet(((Map)o).keySet());
      }
    });

  /** Return the current set of contained Agent's MessageAddresses */
  public Set getAgentAddresses() {
    // memorize to avoid consing a new Set each time.
    return (Set) _getAgentAddressesMemo.eval(getAgents());
  }

  public ComponentDescription getAgentDescription(MessageAddress agentId) {
    Map m = getAgents();
    return (ComponentDescription) m.get(agentId);
  }

  public List getComponents() { // better to use iterator()!
    return super.listComponents();
  }

  protected Iterator getComponentsIterator() {
    return iterator();
  }

  private Memo _getAgentsMemo = Memo.get(new Memo.Function() {
      public String toString() { return "Memo<getAgents>"; }
      public Object eval(Object o) {
        List cds = (List) o;

        HashMap ret = new HashMap(cds.size());
        for (Iterator iter = componentIterator(); iter.hasNext(); ) {
          Object ob = iter.next();
          ComponentDescription desc = (ComponentDescription) ob;
          MessageAddress cid = cdToMa(desc);
          if (cid != null) {
            ret.put(cid, desc);
          }
        }
        return Collections.unmodifiableMap(ret);
      }
    });

  /** return a map of MessageAddress to ComponentDescription which 
   * describes the current set of agents.
   * The returned map is Unmodifiable and will be shared across multiple
   * invocations.
   */
  public synchronized Map getAgents() {
    return (Map) _getAgentsMemo.eval(getBoundComponentList());
  }

  /** Interpret an Agent-level ComponentDescription to 
   * determine the agent name in the form of the MessageAddress of the agent.
   * May return null if uninterpretable.
   */
  protected MessageAddress cdToMa(ComponentDescription desc) {
    Object o = desc.getParameter();
    MessageAddress cid = null;
    if (o instanceof MessageAddress) {
      cid = (MessageAddress) o;
    } else if (o instanceof String) {
      cid = MessageAddress.getMessageAddress((String) o);
    } else if (o instanceof List) {
      List l = (List) o;
      if (l.size() > 0) {
        Object o1 = l.get(0);
        if (o1 instanceof MessageAddress) {
          cid = (MessageAddress) o1;
        } else if (o1 instanceof String) { // primary case
          cid = MessageAddress.getMessageAddress((String) o1);
        } else {
          // shouldn't happen
	  // o1 unknown Object type!
	  Logger logger = Logging.getLogger(AgentManager.class);
	  if (logger.isWarnEnabled())
	    logger.warn("Unknown object class in ComponentDescription List at CID spot: " + o1);
        }
      }
    } else {
      // sometimes we get a null ComponentDesc parameter (e.g. agent binder)
      // so cid will be null. This is OK.
    }

    return cid;
  }

  public void addAgent(MessageAddress agentId, StateTuple tuple) {
    if (containsAgent(agentId)) {
      // agent already exists
      throw new RuntimeException("Agent " + agentId + " already exists");
    }
    
    // add the agent
    if (!add(tuple)) {
      throw new RuntimeException("Agent " + agentId + " returned \"false\"");
    }

    // the agent has started and is now ACTIVE
  }

  public void removeAgent(MessageAddress agentId) {
    // find the agent's component description
    ComponentDescription desc = getAgentDescription(agentId);
    if (desc == null) {
      // no such agent, or not loaded with a desc
      throw new RuntimeException("Agent " + agentId + " is not loaded");
    }

    if (!remove(desc)) {
      throw new RuntimeException("Unable to remove agent " + agentId +
          ", \"remove()\" returned false");
    }

    // the agent has been UNLOADED and removed
  }

  /**
   * Shuts down the local node.
   * <p>
   * 
   * @param localNode the local node to be shutdown.
   */
  protected void shutdownNode(final MessageAddress localNode) {
    synchronized(shutdownLock) {
      if (isNodeShuttingDown) {
        // Shut down sequence has already been initiated.
        return;
      }
      isNodeShuttingDown = true;
    }
    Runnable r = new Runnable() {
      public void run() {
        Iterator it = getAgentAddresses().iterator();
        // Remove all agents, node agent should be last.
        while (it.hasNext()) {
          MessageAddress addr = (MessageAddress)it.next();
          if (addr == null || addr.equals(localNode)) {
            continue;
          }
          removeAgent(addr);
        }
        // Remove node agent.
        removeAgent(localNode);
        
        revokeServices();
        
        // For debugging purposes: verify all services have been revoked
        ServiceBroker sb = getServiceBroker();
        it = sb.getCurrentServiceClasses();
        while (it.hasNext()) {
          Class cl = (Class)it.next();
          Logging.getLogger(this.getClass()).error(
              "Service should have been revoked: " + cl.getName());
        }
      }
    };
    (new Thread(r)).start();
  }

  private ServiceProvider add_service(Class clazz, Service service) {
    // we must use our service broker, otherwise our child components
    // will not be able to block our services
    ServiceBroker sb = getServiceBroker();
    ServiceProvider sp = new SimpleServiceProvider(clazz, service);
    if (!sb.addService(clazz, sp)) {
      throw new RuntimeException("Unable to add service " + clazz);
    }
    return sp;
  }

  private void revoke_service(Class clazz, ServiceProvider sp) {
    if (sp != null) {
      ServiceBroker sb = getServiceBroker();
      sb.revokeService(clazz, sp);
    }
  }

  private static final class SimpleServiceProvider
      implements ServiceProvider {
    private final Class clazz;
    private final Service service;

    public SimpleServiceProvider(Class clazz, Service service) {
      this.clazz = clazz;
      this.service = service;
    }

    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (clazz.isAssignableFrom(serviceClass)) {
        return service;
      } else {
        return null;
      }
    }

    public void releaseService(ServiceBroker sb, Object requestor,
                               Class serviceClass, Object service) {
    }
  }
}
