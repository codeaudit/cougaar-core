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
package org.cougaar.core.society;

import org.cougaar.core.component.*;

import org.cougaar.core.agent.AgentManagerBindingSite;
import org.cougaar.core.logging.LoggingService;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.PolicyMulticastMessage;
import org.cougaar.domain.planning.ldm.policy.Policy;

/** The NodeTrust Component implementation.
 * For now this is a component that acts as a proxy
 * for node to receive node level messages and provide
 * node level services.
 * For now it doesn't actually contain anything - at least not any subcomponents.
 **/
public class NodeTrustComponent
  extends ContainerSupport
  implements StateObject, MessageTransportClient, NodePolicyWatcher
{
  private AgentManagerBindingSite bindingSite = null;
  private Object loadState = null;
  private TrustStatusServiceImpl theTSS;
  private TrustStatusServiceProvider tssSP;
  private MessageTransportService messageTransService;
  private LoggingService logging;
  private MessageAddress myaddress;

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    if (bs instanceof AgentManagerBindingSite) {
      bindingSite = (AgentManagerBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+"into " + bs);
    }
  }

  public void setState(Object loadState) {
    this.loadState = loadState;
  }

  public Object getState() {
    //for now we won't keep any state
    return null;
  }

  public void load() {
    super.load();

    //if we were doing something with state... use it here
    // then reset it.
    loadState = null;

    ServiceBroker sb = bindingSite.getServiceBroker();

    // create the TrustStatusService implementation
    theTSS = new TrustStatusServiceImpl(); 
    // create the TrustStatusServiceProvider
    tssSP = new TrustStatusServiceProvider(theTSS);
    //add the service to the Node ServiceBroker
    sb.addService(TrustStatusService.class, tssSP);

    // setup and register message transport service
    messageTransService = (MessageTransportService)
      sb.getService(this, MessageTransportService.class, 
                                    new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          if (MessageTransportService.class.equals(re.getService())) {
            messageTransService = null;
          }
        }
      });    
    messageTransService.registerClient(this);

    // setup the logging service
    logging = (LoggingService)
      sb.getService(this, LoggingService.class, 
                                    new ServiceRevokedListener() {
        public void serviceRevoked(ServiceRevokedEvent re) {
          if (LoggingService.class.equals(re.getService())) {
            logging = null;
          }
        }
      });
    //System.out.println("\n Loaded NodeTrustComponent");
  }

  public void unload() {
    super.unload();
    
    // unload services in reverse order of "load()"
    ServiceBroker sb = bindingSite.getServiceBroker();
    sb.revokeService(TrustStatusService.class, tssSP);
    // release services
    sb.releaseService(this, MessageTransportService.class, messageTransService);
    sb.releaseService(this, LoggingService.class, logging);
  }

  //
  // binding services
  //

  protected final AgentManagerBindingSite getBindingSite() {
    return bindingSite;
  }
  protected String specifyContainmentPoint() {
    return "Node.NodeTrust";
  }
  protected ContainerAPI getContainerProxy() {
    return null;
  }

  //implement messagetransportclient interface
  public void receiveMessage(Message message) {
    boolean found = false;
    if (message instanceof PolicyMulticastMessage) {
      PolicyMulticastMessage pmm = (PolicyMulticastMessage) message;
      Policy policy = pmm.getPolicy();
      if (policy instanceof NodeTrustPolicy) {
        NodeTrustPolicy ntp = (NodeTrustPolicy) policy;
        String category = ntp.getTrustCategory();
        if (category.equals(NodeTrustPolicy.SOCIETY)) {
          int level = ntp.getTrustLevel();
          // set this on the TrustStatusService
          theTSS.changeSocietyTrust(level);
          found = true;
          System.out.println("\n NODETRUSTCOMPONENT recieved a message");
        }
      }
    }
    if (!found) {
      //drop it on the floor right now 
      // to  be implemented later so print out a warning
     logging.warning("\n!!!" + this + "Received a Message that it doesn't know" +
                         " how to process.");
    }
  }

  public MessageAddress getMessageAddress() {
    if (myaddress != null) {
      return myaddress;
    } else {
      //create it
      String nodename = getBindingSite().getIdentifier();
      myaddress = new MessageAddress(nodename+"-Policy");
      return myaddress;
    }
  }
    

}
