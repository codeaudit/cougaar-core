/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.society.*;

import java.beans.*;
import java.lang.reflect.*;


/** A container for Agent Components.
 **/
public class AgentManager 
  extends ContainerSupport
  implements ContainerAPI
{
    public AgentManager() {
    if (!attachBinderFactory(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }
  }

  /** this constructor used for backwards compatability mode.  Goes away
   * when we are a contained by a Node component
   **/
  public AgentManager(ComponentDescription comdesc) {
    if (!attachBinderFactory(new AgentBinderFactory())) {
      throw new RuntimeException("Failed to load the AgentBinderFactory");
    }
  }

 private AgentManagerBindingSite bindingSite = null;

  public final void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentManagerBindingSite) {
      bindingSite = (AgentManagerBindingSite) bs;
      setChildServiceBroker(new AgentManagerServiceBroker(bindingSite));
    } else {
      throw new RuntimeException("Tried to laod "+this+"into "+bs);
    }

    // We cannot start adding services until after the serviceBroker has been created.
    // add some services for the agents (clusters).
    // maybe this can be hooked in from Node soon.
    //childContext.addService(MetricsService.class, new MetricsServiceProvider(agent));
    //childContext.addService(MessageTransportService.class, new MessageTransportServiceProvider(agent));

  }

  protected final AgentManagerBindingSite getBindingSite() {
    return bindingSite;
  }


  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager";
  }

  protected ContainerAPI getContainerProxy() {
    return new AgentManagerProxy();
  }
  
  public void requestStop() { }

 /**
  * Add a cluster
  */
  public boolean add(Object obj) {
    //System.err.print("AgentManager adding Cluster");
    ClusterServesClusterManagement cluster = null;
    if (obj instanceof ComponentDescription) {
     ComponentDescription desc = (ComponentDescription) obj;
      try {
        String insertionPoint = desc.getInsertionPoint();
        if (!("Node.AgentManager.Agent".equals(insertionPoint))) {
          // fix to support non-agent components
          throw new IllegalArgumentException("Currently only agent ADD is supported, not "+ insertionPoint);
        }
        cluster = createCluster(desc); // calls super.add(cluster);
      } catch (Exception e) {
      System.err.println("\nUnable to load cluster["+desc+"]: "+e);
      e.printStackTrace();
      }
      return hookupCluster(cluster);
    } else if (obj instanceof ClusterServesClusterManagement) {
      return hookupCluster((ClusterServesClusterManagement)obj);
    } else {
      //just bail if this is a weird object for now
      System.err.println("Warning AgentManager can not add the following object: " +obj);
      return false;
    }
  }

  private boolean hookupCluster(ClusterServesClusterManagement cluster) {
    ClusterIdentifier cid = cluster.getClusterIdentifier();
    String cname = cid.toString();
    //System.err.print("\n\t"+cname);
    //System.err.print("\nLoading Plugins:");
    try {
      // parse the cluster properties
      // currently assume ".ini" files
      InputStream in = ConfigFinder.getInstance().open(cname+".ini");
      ComponentDescription[] cDescs = 
        org.cougaar.core.society.INIParser.parse(in, "Node.AgentManager.Agent");
      
      // add the plugins and other cluster components
      //
      // FIXME could benefit from a bulk-add message
      for (int j = 0; j < cDescs.length; j++) {
        ComponentMessage addCM = 
          new ComponentMessage(
                               cid,
                               cid,
                               ComponentMessage.ADD,
                               cDescs[j]);
        // bypass the message system to initialize the cluster
        cluster.receiveMessage(addCM);
      }

      // tell the cluster to proceed.
      ClusterInitializedMessage m = new ClusterInitializedMessage();
      m.setOriginator(cid);
      m.setTarget(cid);
      cluster.receiveMessage(m);
    } catch (Exception e) {
      System.err.println("\nUnable to add cluster["+cluster+"] child omponents: "+e);
      e.printStackTrace();
    }

    //System.err.println("\nPlugins Loaded.");
    // register cluster with Node's ExternalNodeActionListener
    getBindingSite().registerCluster(cluster);
    //ExternalNodeActionListener eListener = getBindingSite().getExternalNodeActionListener();
    // notify the listener
    // if (eListener != null) {
//       try {
//         eListener.handleClusterAdd(eController, cid);
//       } catch (Exception e) {
//         // lost listener?  should we kill this Node?
//         System.err.println("Lost connection to external listener? "+e.getMessage());
//       }
//     }
    // if we are all the way to this point return true
    return true;
  }


  /**
   * Create a Cluster from a ComponentDescription.
   */
  private ClusterServesClusterManagement createCluster( ComponentDescription desc) 
  {

    // check the cluster classname
    String clusterClassname = desc.getClassname();

    // load an instance of the cluster
    //
    // FIXME use the "desc.getCodebase()" and other arguments
    ClusterServesClusterManagement cluster;
    try {
      Class clusterClass = Class.forName(clusterClassname);
      Object clusterInstance = clusterClass.newInstance();
      if (!(clusterInstance instanceof ClusterServesClusterManagement)) {
        throw new ClassNotFoundException();
      }
      cluster = (ClusterServesClusterManagement)clusterInstance;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalArgumentException(
          "Unable to load agent class: \""+clusterClassname+"\"");
    }

    // the parameter should be the cluster name
    String clusterid;
    try {
      clusterid = (String)((List)desc.getParameter()).get(0);
    } catch (Exception e) {
      clusterid = null;
    }
    if (clusterid == null) {
      throw new IllegalArgumentException(
          "Agent specification lacks a String \"name\" parameter");
    }
   
    // set the ClusterId
    ClusterIdentifier cid = new ClusterIdentifier(clusterid);
    cluster.setClusterIdentifier(cid);

    //move the cluster to the intialized state
    super.add(cluster);
    if (cluster.getState() != GenericStateModel.ACTIVE) {
      System.err.println("Cluster "+cluster+" is not Active!");
    }

    return cluster;
  }

  public String getName() {
    return getBindingSite().getName();
  }


  //
  // support classes
  //

  private static class AgentManagerServiceBroker 
    extends PropagatingServiceBroker
  {
    public AgentManagerServiceBroker(BindingSite bs) {
      super(bs);
    }
  }
 

  private class AgentManagerProxy implements AgentManagerForBinder, 
                                             ClusterManagementServesCluster, BindingSite {

    public String getName() {return AgentManager.this.getName(); }
    
    // BindingSite
    public ServiceBroker getServiceBroker() {
      return AgentManager.this.getServiceBroker();
    }
    public void requestStop() {}
    public boolean remove(Object o) {return true; }
  }

//   public boolean add(Object o) {
//     // this could go away if we turned the message into a ComponentDescription object.
//     return true;
//   }

//   public boolean remove(Object o) {
//     return true;
//   }
  
  //is this node level or agent level???
  //public ConfigFinder getConfigFinder() {
  //  return agent.getConfigFinder();
  //}


}

