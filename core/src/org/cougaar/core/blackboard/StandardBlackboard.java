/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.blackboard;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.AgentChildBindingSite;
import org.cougaar.domain.planning.ldm.Domain;
import org.cougaar.domain.planning.ldm.DomainManager;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.agent.*;
import java.beans.*;
import java.lang.reflect.*;

import org.cougaar.core.cluster.persist.BasePersistence;
import org.cougaar.core.cluster.persist.DatabasePersistence;
import org.cougaar.core.cluster.persist.Persistence;
import org.cougaar.core.cluster.persist.PersistenceException;


/** The standard Blackboard Component implementation.
 * For now it just looks like a container but doesn't
 * actually contain anything - at least not any subcomponents.
 **/
public class StandardBlackboard
  extends ContainerSupport
{
  private AgentChildBindingSite bindingSite = null;
  private Blackboard bb = null;
  private Distributor d = null;
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentChildBindingSite) {
      bindingSite = (AgentChildBindingSite) bs;
      bb = new Blackboard(bindingSite.getCluster());
      d = bb.getDistributor();
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bs);
    }
  }

  public void load() {
    super.load();

    bb.init();
    ServiceBroker sb = bindingSite.getServiceBroker();

    // offer hooks back to the Agent
    sb.addService(BlackboardForAgent.class,
                  new BlackboardForAgentServiceProvider(bb));

    //offer Blackboard service
    sb.addService(BlackboardService.class,
                  new BlackboardServiceProvider(bb.getDistributor()));

    // load the domains & lps
    try {
      Collection domains = DomainManager.values();
      // MIK BB
      // HACK to let Metrics see plan objects
      //Domain rootDomain = DomainManager.find("root"); 

      for (Iterator i = domains.iterator(); i.hasNext(); ) {
        Domain d = (Domain) i.next();

        bb.connectDomain(d);

        // Replace HACK to let Metrics count plan objects - there should be
        // a blackboard-level metrics service which deals with this.
        //if (d == rootDomain) {
        //  myLogPlan = (LogPlan) bb.getXPlanForDomain(d);
        //}
      }

      // MIK BB
      /*
      // specialLPs - maybe it should have a special domain.  MIK
      if (isMetricsHeartbeatOn) {
        bb.addLogicProvider(new MetricsLP(myLogPlan, this));
      }
      */
    } catch (Exception e) { 
      synchronized (System.err) {
        System.err.println("Problem loading Blackboard domains: ");
        e.printStackTrace(); 
      }
    }

    // add services here (none for now)
  }

  //
  // binding services
  //

  protected final AgentChildBindingSite getBindingSite() {
    return bindingSite;
  }
  protected String specifyContainmentPoint() {
    return "Node.AgentManager.Agent.Blackboard";
  }
  protected ContainerAPI getContainerProxy() {
    return null;
  }

  //
  // blackboardforagent support
  //
  private static class BlackboardForAgentServiceProvider 
    implements ServiceProvider 
  {
    Blackboard blackboard;
    BlackboardForAgentServiceProvider(Blackboard blackboard) {
      this.blackboard = blackboard;
    }
    public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
      if (serviceClass == BlackboardForAgent.class) {
        if (requestor instanceof Agent) {
          return new BlackboardForAgentImpl(blackboard);
        }
      }
      return null;
    }

    public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
      if (service instanceof BlackboardForAgentImpl) {
        ((BlackboardForAgentImpl)service).release(blackboard);
      }
    }
  }

  private static class BlackboardForAgentImpl 
    implements BlackboardForAgent
  {
    private Blackboard blackboard;
    private BlackboardForAgentImpl(Blackboard bb) {
      this.blackboard = bb;
    }

    void release(Blackboard bb) {
      if (bb == blackboard) {
        this.blackboard = null;
      } else {
        throw new RuntimeException("Illegal attempt to revoke a "+this+".");
      }
    }
    // might be better for blackboard to be a messagetransport client, eh?
    public void receiveMessages(List messages) {
      blackboard.getDistributor().receiveMessages(messages);
    }
    // not sure if this is really needed - check out ClusterImpl.getDatabaseConnection()
    public Persistence getPersistence() {
      return blackboard.getDistributor().getPersistence();
    }
  }
}
