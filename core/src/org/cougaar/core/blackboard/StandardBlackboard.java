/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.blackboard;

import org.cougaar.core.service.*;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

import java.io.InputStream;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.AgentChildBindingSite;
import org.cougaar.core.domain.Domain;
import org.cougaar.core.domain.DomainManager;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import java.beans.*;
import java.lang.reflect.*;

import org.cougaar.core.persist.BasePersistence;
import org.cougaar.core.persist.DatabasePersistence;
import org.cougaar.core.persist.Persistence;
import org.cougaar.core.persist.PersistenceException;


/** The standard Blackboard Component implementation.
 * For now it just looks like a container but doesn't
 * actually contain anything - at least not any subcomponents.
 **/
public class StandardBlackboard
  extends ContainerSupport
  implements StateObject
{
  private AgentChildBindingSite bindingSite = null;
  private Object loadState = null;
  private Blackboard bb = null;
  private Distributor d = null;

  private BlackboardForAgentServiceProvider bbAgentSP;
  private BlackboardServiceProvider bbSP;
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentChildBindingSite) {
      bindingSite = (AgentChildBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bs);
    }
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  public Object getState() {
    try {
      return bb.getState();
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to capture Blackboard state", e);
    }
  }

  public void load() {
    super.load();
    ServiceBroker sb = bindingSite.getServiceBroker();

    // create blackboard with optional prior-state
    bb = new Blackboard(bindingSite.getCluster(), sb, loadState);
    loadState = null;

    bb.init();
    d = bb.getDistributor();

    bb.connectDomains();

    // offer hooks back to the Agent
    bbAgentSP = new BlackboardForAgentServiceProvider(bb);
    sb.addService(BlackboardForAgent.class, bbAgentSP);

    //offer Blackboard service and Blackboard metrics service
    // both use the same service provider
    bbSP = new BlackboardServiceProvider(bb.getDistributor());
    sb.addService(BlackboardService.class, bbSP);
    sb.addService(BlackboardMetricsService.class, bbSP);
    sb.addService(BlackboardQueryService.class, bbSP);

    // add services here (none for now)
  }

  public void unload() {
    super.unload();
    
    // unload services in reverse order of "load()"
    ServiceBroker sb = bindingSite.getServiceBroker();
    sb.revokeService(BlackboardMetricsService.class, bbSP);
    sb.revokeService(BlackboardService.class, bbSP);
    sb.revokeService(BlackboardForAgent.class, bbAgentSP);

    bb.stop();
  }

  //
  // binding services
  //

  protected final AgentChildBindingSite getBindingSite() {
    return bindingSite;
  }
  protected String specifyContainmentPoint() {
    return Blackboard.INSERTION_POINT;
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

    public void restartAgent(MessageAddress cid) {
      blackboard.getDistributor().restartAgent(cid);
    }
  }
}

