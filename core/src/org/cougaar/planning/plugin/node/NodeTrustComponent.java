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

package org.cougaar.planning.plugin.node;

import java.io.PrintStream;
import org.cougaar.core.agent.AgentChildBindingSite;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ContainerSupport;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.StateObject;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.node.*;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.planning.ldm.policy.Policy;

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
  private AgentChildBindingSite bindingSite = null;
  private Object loadState = null;
  private TrustStatusServiceImpl theTSS;
  private TrustStatusServiceProvider tssSP;
  private MessageTransportService messageTransService;
  private LoggingService logging;
  private MessageAddress myaddress;

  public void setBindingSite(BindingSite bs) {
    super.setBindingSite(bs);
    bindingSite = (AgentChildBindingSite) bs; // allow ClassCastException to pass up
  }

  private NodeIdentificationService nodeIdentificationService = null;
  public void setNodeIdentificationService(NodeIdentificationService nis) {
    nodeIdentificationService = nis;
  }
  protected NodeIdentificationService getNodeIdentificationService() {
    return nodeIdentificationService;
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

  protected final AgentChildBindingSite getBindingSite() {
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
      // don't do anything with this message since we don't know
      // about its type
     logging.debug("\n!!!" + this + "Received a Message that it doesn't know" +
                         " how to process: " + message);
    }
  }

  public MessageAddress getMessageAddress() {
    if (myaddress != null) {
      return myaddress;
    } else {
      //create it
      String name = getNodeIdentificationService().getMessageAddress().toString();
      myaddress = MessageAddress.getMessageAddress(name+"-Policy");
      return myaddress;
    }
  }
    

}
