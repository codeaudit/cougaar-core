/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.util.List;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.agent.service.MessageSwitchService;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.service.BlackboardMetricsService;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.persist.PersistenceObject;

/** The standard Blackboard Component implementation.
 * For now it just looks like a container but doesn't
 * actually contain anything - at least not any subcomponents.
 **/
public class StandardBlackboard
  extends ContainerSupport
  implements StateObject
{
  private ServiceBroker sb = null;
  private BindingSite bindingSite = null;
//   private Object loadState = null;
  private Blackboard bb = null;
  private Distributor d = null;

  private MessageSwitchService msgSwitch;

  private BlackboardForAgentServiceProvider bbAgentSP;
  private BlackboardServiceProvider bbSP;
  
  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    this.bindingSite = bs;
    this.sb = bs.getServiceBroker();
  }

  public void setState(Object loadState) {
//     this.loadState = loadState;
  }

  public Object getState() {
    return null;
//     try {
//       return bb.getState();
//     } catch (Exception e) {
//       throw new RuntimeException(
//           "Unable to capture Blackboard state", e);
//     }
  }
  public PersistenceObject getPersistenceObject() {
    try {
      return bb.getPersistenceObject();
    } catch (Exception e) {
      throw new RuntimeException("Unable to capture Blackboard state", e);
    }
  }

  public void load() {
    super.load();

    msgSwitch = (MessageSwitchService)
      sb.getService(this, MessageSwitchService.class, null);
    if (msgSwitch == null) {
      throw new RuntimeException(
          "Unable to obtain MessageSwitchService, which is required"+
          " for the blackboard to send messages!");
    }

    // create blackboard with optional prior-state
    bb = new Blackboard(msgSwitch, sb, null);
//     bb = new Blackboard(msgSwitch, sb, loadState);
//     loadState = null;

    bb.init();
    d = bb.getDistributor();
//     d.getPersistence().registerServices(sb);

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
    sb.revokeService(BlackboardMetricsService.class, bbSP);
    sb.revokeService(BlackboardService.class, bbSP);
    sb.revokeService(BlackboardForAgent.class, bbAgentSP);
//     d.getPersistence().unregisterServices(sb);

    bb.stop();
    if (msgSwitch != null) {
      sb.releaseService(
          this, MessageSwitchService.class, msgSwitch);
      msgSwitch = null;
    }
  }

  //
  // binding services
  //

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
    // might be better for blackboard to be a message switch handler, eh?
    public void receiveMessages(List messages) {
      blackboard.getDistributor().receiveMessages(messages);
    }

    public void restartAgent(MessageAddress cid) {
      blackboard.getDistributor().restartAgent(cid);
    }

    public PersistenceObject getPersistenceObject() {
      return blackboard.getDistributor().getPersistenceObject();
    }

    public void persistNow() {
      blackboard.getDistributor().persistNow();
    }
  }
}

