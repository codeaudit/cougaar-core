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

package org.cougaar.core.node;

import org.cougaar.core.agent.SimpleAgent;

import org.cougaar.core.service.*;
import org.cougaar.core.mts.*;

import org.cougaar.core.thread.ThreadServiceProvider;

import org.cougaar.core.service.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.service.alarm.*;
import org.cougaar.core.agent.service.containment.*;
import org.cougaar.core.agent.service.democontrol.*;
import org.cougaar.core.agent.service.registry.*;
import org.cougaar.core.agent.service.scheduler.*;
import org.cougaar.core.agent.service.uid.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.agent.ClusterServesClusterManagement;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.service.NamingService;

import org.cougaar.bootstrap.SystemProperties;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;

import java.util.*;
import javax.naming.directory.DirContext;
import javax.naming.*;

import org.cougaar.util.*;

import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.AgentBindingSite;

import org.cougaar.core.component.*;

import org.cougaar.core.service.NamingService;

import org.cougaar.core.node.ComponentMessage;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageStatistics;

// cluster context registration
import org.cougaar.core.agent.ClusterContext;

// blackboard support
import org.cougaar.core.blackboard.BlackboardForAgent;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.blackboard.BlackboardServiceProvider;

// message-transport support
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.mts.MessageTransportWatcher;
import org.cougaar.core.service.MessageWatcherService;

// LDM service
import org.cougaar.core.plugin.LDMService;
import org.cougaar.core.plugin.LDMServiceProvider;
import org.cougaar.core.plugin.PluginManager;

// prototype and property providers
import org.cougaar.core.plugin.LatePropertyProvider;
import org.cougaar.core.plugin.PrototypeProvider;
import org.cougaar.core.plugin.PropertyProvider;

// Persistence
//  import org.cougaar.core.persist.DatabasePersistence;
import org.cougaar.core.persist.Persistence;

import org.cougaar.core.service.*;
import org.cougaar.core.mts.*;

import org.cougaar.core.thread.ThreadServiceProvider;

import org.cougaar.core.qos.monitor.QosMonitorService;
import org.cougaar.core.qos.monitor.QosMonitorServiceProvider;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.qos.metrics.MetricsUpdateService;
import org.cougaar.core.qos.metrics.MetricsServiceProvider;

import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.service.MessageStatisticsService;
import org.cougaar.core.service.MessageWatcherService;

import org.cougaar.core.agent.ClusterServesClusterManagement;
import org.cougaar.core.naming.NamingServiceProvider;
import org.cougaar.core.service.NamingService;

import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;


import org.cougaar.core.component.*;

import java.io.Serializable;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import java.security.*;
import java.security.cert.*;
import javax.naming.NamingException;

import org.cougaar.core.agent.AgentManager;
import org.cougaar.core.agent.ClusterInitializedMessage;

import org.cougaar.core.agent.*;
import org.cougaar.util.*;

import org.cougaar.core.component.*;
import java.beans.Beans;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.LoggerController;
import org.cougaar.util.log.LogTarget;

/**
 * Implementation of an Agent which manages the resources and capabilities of a node.
 */
public class NodeAgent
  extends SimpleAgent
{
  private ServiceBroker agentServiceBroker = null;
  private AgentManager agentManager = null;

  /** A reference to the MessageTransportService containing the Messenger **/
  private transient MessageTransportService theMessenger = null;


  /** @param asb An unproxied reference to the top-level ServiceBroker so that we can 
   * add global services.
   * @param am An unproxied reference to the AgentManager so that we can add agents.
   **/
  public NodeAgent(ServiceBroker asb, AgentManager am) {
    agentServiceBroker = asb;
    agentManager = am;
  }

  /*
  public NodeAgent(ComponentDescription comdesc) {
    super(comdesc);
  }
  */

  public void load() 
    throws StateModelException 
  {
    ServiceBroker localsb = getServiceBroker();
    ServiceBroker rootsb = agentServiceBroker;
    AgentManager am = agentManager;

    String name;
    NodeIdentifier id;
    try {
      NodeIdentificationService nis = (NodeIdentificationService) rootsb.getService(this,NodeIdentificationService.class,null);
      if (nis != null) {
        id = nis.getNodeIdentifier();
        name = id.toString();
      } else {
        throw new RuntimeException("No node name specified");
      }
    } catch (RuntimeException e) {
      throw new Error("Couldn't figure out Node name "+e);
    }

    // set the MessageAddress to be a cid for now (sigh)
    setMessageAddress( new ClusterIdentifier(name) );

    // set up our binder factory
    {
      BinderFactory nabf = new NodeAgentBinderFactory();
      if (!attachBinderFactory(nabf)) {
        throw new Error("Failed to load the NodeAgentBinderFactory in NodeAgent");
      }
    }

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
          public void releaseService(ServiceBroker xsb, Object requestor, Class serviceClass, Object service) {
          }
        };
      getServiceBroker().addService(NodeControlService.class, ncsp);
    }

    /*
    // security manager
    {
      String smn = System.getProperty(SecurityComponent.SMC_PROP,
                                      "org.cougaar.core.node.StandardSecurityComponent");
      if (smn != null) {
        try {
          Class smc = Class.forName(smn);
          if (SecurityComponent.class.isAssignableFrom(smc)) {
            ComponentDescription smcd = 
              new ComponentDescription(
                                       id.toString()+"SecurityComponent",
                                       "Node.AgentManager.Agent.SecurityComponent",
                                       smn,
                                       null,  //codebase
                                       null,  //parameters
                                       null,  //certificate
                                       null,  //lease
                                       null); //policy
            super.add(smcd);
          } else {
            System.err.println("Error: SecurityComponent specified as "+smn+" which is not an instance of SecurityComponent");
            System.exit(1);
          }
        } catch (Exception e) {
          System.err.println("Error: Could not load SecurityComponent "+smn+": "+e);
          e.printStackTrace();
          System.exit(1);
        }
      }
    }
    */

    ThreadServiceProvider tsp = new ThreadServiceProvider(rootsb, "Node " + name);
    tsp.provideServices(rootsb);

    try {
      rootsb.addService(NamingService.class,
                    new NamingServiceProvider(SystemProperties.getSystemPropertiesWithPrefix("javax.naming.")));
    } catch (javax.naming.NamingException ne) {
      throw new Error("Couldn't initialize NamingService "+ne);
    }

    try {
      LoggingServiceProvider loggingServiceProvider = 
        new LoggingServiceProvider(SystemProperties.getSystemPropertiesWithPrefix("org.cougaar.core.logging."));
      rootsb.addService(LoggingService.class,
                    loggingServiceProvider);
      rootsb.addService(LoggingControlService.class,
                    loggingServiceProvider);
    } catch (java.io.IOException ioe) {
      throw new Error("Couldn't initialize LoggingService "+ioe);
    }

    MetricsServiceProvider msp = new MetricsServiceProvider(rootsb, id);
    rootsb.addService(MetricsService.class, msp);
    rootsb.addService(MetricsUpdateService.class, msp);


    //add the vm metrics
    rootsb.addService(NodeMetricsService.class,
                  new NodeMetricsServiceProvider(new NodeMetricsProxy()));

    String filename = System.getProperty("org.cougaar.filename");
    String experimentId = System.getProperty("org.cougaar.experiment.id");
    if (filename == null) {
      if (experimentId == null) {
        // use the default "name.ini"
        filename = name + ".ini";
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


    ComponentDescription[] agentDescs;
    try {
      ServiceProvider sp;
      if (filename != null) {
        sp = new FileInitializerServiceProvider();
      } else {
        sp = new DBInitializerServiceProvider(experimentId);
      }
      rootsb.addService(InitializerService.class, sp);

      InitializerService is = (InitializerService) rootsb.getService(this, InitializerService.class, null);
      agentDescs =
        is.getComponentDescriptions(name, "Node.AgentManager");
      rootsb.releaseService(this, InitializerService.class, is);
    } catch (Exception e) {
      throw new Error("Couldn't initialize NodeAgent from InitializerService "+e);
    }


    // Set up MTS and QOS service provides.
    //
    // NB: The order is important for now - MTS *must* be created
    // first.
    initTransport(rootsb, id);  

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
    super.add(ntcdesc);         // let a ComponentLoadFailure pass through

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
        super.add(nsscDesc);
      } catch (RuntimeException re) {
        re.printStackTrace();
      }
    }

    super.load();

    // load the clusters
    //
    // once bulk-add ComponentMessages are implements this can
    //   be done with "this.receiveMessage(compMsg)"
    addAgents(agentDescs);

    //mgmtLP = new MgmtLP(this); // MTMTMT turn off till RMI namespace works
  }

  public boolean add(Object o) {
    return super.add(o);
  }

  // replace with Container's add, but keep this basic code
  protected void addAgent(ComponentDescription desc) {
    // simply wrap as a single-element "bulk" operation
    ComponentDescription[] descs = new ComponentDescription[1];
    descs[0] = desc;
    addAgents(descs);
  }
 
  /**
   * Add Clusters and their child Components (Plugins, etc) to this Node.
   * <p>
   * Note that this is a bulk operation, since the loading process is:<ol>
   *   <li>Create the empty clusters</li>
   *   <li>Add the Plugins and initialize the clusters</li>
   * </ol>
   * <p>
   */
  protected void addAgents(ComponentDescription[] descs) {
    int nDescs = ((descs != null) ? descs.length : 0);
    //System.err.print("Creating Clusters:");
    for (int i = 0; i < nDescs; i++) {
      ComponentDescription desc = descs[i];
      try {
        //Let the agentmanager create the cluster
        agentManager.add(desc);
      } catch (Exception e) {
        System.err.println("Exception creating component ("+desc+"): " + e);
        e.printStackTrace();
      }
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

    initQos(rootsb, mtsp, id);
  }

  private void initQos (ServiceBroker rootsb, MessageTransportServiceProvider mtsp, NodeIdentifier id) {
    String name = id.toString();
    QosMonitorServiceProvider qmsp = new QosMonitorServiceProvider(name, mtsp);
    add(qmsp);
    rootsb.addService(QosMonitorService.class, qmsp);
  }

  private class MTSClient implements MessageTransportClient {
    public void receiveMessage(Message message) {
      NodeAgent.this.receiveMessage(message);
    }

    public MessageAddress getMessageAddress() {
      return NodeAgent.this.getMessageAddress();
    }
  }
  public void receiveMessage(final Message m) {
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
          if (!("Node.Agent".equals(ip))) {
            throw new UnsupportedOperationException(
              "Only Agent ADD supported for now, not "+ip);
          }
          agentManager.add(st);
        } else {
          // not implemented yet!  will be okay once Node is a Container
          throw new UnsupportedOperationException(
            "Unsupported ComponentMessage: "+m);
        }
      } else if (m instanceof AgentManagementMessage) {
        // run in a separate thread (in case the source is local)
        Runnable r = new Runnable() {
          public void run() {
            if (m instanceof MoveAgentMessage) {
              MoveAgentMessage mam = (MoveAgentMessage) m;
              agentManager.moveAgent(
                  mam.getAgentIdentifier(), 
                  mam.getNodeIdentifier());
            } else if (m instanceof CloneAgentMessage) {
              CloneAgentMessage cam = (CloneAgentMessage) m;
              agentManager.cloneAgent(
                  cam.getAgentIdentifier(), 
                  cam.getNodeIdentifier(),
                  cam.getCloneAgentIdentifier(),
                  cam.getCloneBlackboard());
            } else {
              // ignore
            }
          }
        };
        Thread t = new Thread(r, m.toString());
        t.start();
      } else if (m.getTarget().equals(MessageAddress.SOCIETY)) {
        // we don't do anything with these. ignore it.
      } else {
        throw new UnsupportedOperationException(
            "Unsupported Message: "+
            ((m != null) ? m.getClass().getName() : "null"));
      }
    } catch (Exception e) {
      System.err.println("Node received invalid message: "+e.getMessage());
      e.printStackTrace();
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
    implements NodeAgentBindingSite
  {
    public NodeAgentBinder(BinderFactory bf, Object child) {
      super(bf, child);
    }
    protected BindingSite getBinderProxy() {
      return this;
    }
  }

}
