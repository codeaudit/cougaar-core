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
import org.cougaar.core.mts.MessageTransportService;

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
     // tell the cluster to proceed.
     try {
       ClusterInitializedMessage m = new ClusterInitializedMessage();
       m.setOriginator(cid);
       m.setTarget(cid);
       cluster.receiveMessage(m);

       // register cluster with Node's ExternalNodeActionListener
       getBindingSite().registerCluster(cluster);

     } catch (Exception e) {
       System.err.println("\nUnable to initialize and register cluster["+cluster+"]  "+e);
       e.printStackTrace();
     }
     
     // if we are all the way to this point return true
     return true;
  }

  private static void debugState(Object state, String path) {
    if (state instanceof ComponentDescription[]) {
      ComponentDescription[] descs = 
	(ComponentDescription[])state;
      for (int i = 0; i < descs.length; i++) {
	ComponentDescription di = descs[i];
	String prefix = path+"["+i+" / "+descs.length+"]";
        if (di == null) {
	  System.out.println(
			     prefix+": null");
	} else {
	  System.out.println(
			     prefix+": "+
			     di.getInsertionPoint()+" = "+
			     di.getClassname()+" "+
			     di.getParameter());
	  if (di.getState() != null) {
	    debugState(di.getState(), prefix);
	  }
	}
      }
    } else {
      System.out.println(path+" non-CD[] "+state);
    }
  }

  /**
   * Support Node-issued agent mobility requests.
   * <p>
   * @param agentID agent to move
   * @param nodeID destination node address
   */
  public void moveAgent(
      ClusterIdentifier agentID,
      NodeIdentifier nodeID) {

    // check parameters, security, etc
    if ((agentID == null) ||
	(nodeID == null)) {
      // error
      return;
    }

    // lookup the agent on this node
    ClusterServesClusterManagement agent = null;
    Iterator iter = super.iterator();
    while (true) {
      if (!(iter.hasNext())) {
	// no such agent?
	return;
      }
      Object oi = iter.next();
      if (oi instanceof ClusterServesClusterManagement) {
	ClusterServesClusterManagement ci =
	  (ClusterServesClusterManagement)oi;
	ClusterIdentifier cid =	ci.getClusterIdentifier();
	if (agentID.equals(cid)) {
	  // found our agent
	  agent = ci;
	  break;
	}
      }
    }

    // suspend the agent's activity, prepare for state capture

    // recursively gather the agent state
    Object state = 
      ((agent instanceof StateObject) ?
       ((StateObject)agent).getState() :
       null);

    System.out.println("state is: "+state);
    debugState(state, "");

    // create a ComponentDescription for the agent, set it's state
    Vector param = new Vector(1);
    param.add(agentID.toString());
    ComponentDescription cd =
      new ComponentDescription(
			       "org.cougaar.core.cluster.ClusterImpl",
			       "Node.AgentManager.Agent",
			       "org.cougaar.core.cluster.ClusterImpl",
			       null,
			       param,
			       null, // certificate
			       null, // lease
			       null, // policy
			       state);

    // create an ADD ComponentMessage with the ComponentDescription
    ComponentMessage addMsg =
      new ComponentMessage(
        new NodeIdentifier(bindingSite.getIdentifier()),
        nodeID,
        ComponentMessage.ADD,
        cd);

    // get the message transport
    MessageTransportService mts = (MessageTransportService)
      getServiceBroker().getService(
				    this,
				    MessageTransportService.class,
				    null);
    if (mts == null) {
      // error!  we should have requested this earlier...
      System.err.println("Unable to get MessageTransport for mobility message");
      return;
    }

    // send message to destination node
    mts.sendMessage(addMsg);

    // wait for add acknowledgement -- postponed to 8.6+

    // destroy the original agent on this node

    System.out.println(
        "Move "+agentID+" to "+nodeID);
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
    if (cluster.getModelState() != GenericStateModel.ACTIVE) {
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

