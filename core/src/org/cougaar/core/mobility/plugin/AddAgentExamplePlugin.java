/*
 * <copyright>
 *  Copyright 2000-2002 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.plugin;

import java.util.*;
import java.io.*;
import org.cougaar.core.mobility.AddTicket;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.ldm.*;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.service.DomainService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.blackboard.IncrementalSubscription;

/*
 * Example plugin to demonstrate how one would add agents to a node
 * by first creating an AddTicket and using the MobilitySupport to create an
 * AgentControl request, the object through which all adds/moves/removes are 
 * recognized by the mobility API and handled correctly and securely.
 *
 * Also shows how one would subscribe to the changed AgentControl objects and 
 * their status. 
 */
public class AddAgentExamplePlugin extends ComponentPlugin {
  
  protected UnaryPredicate AGENT_CONTROL_PRED =
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  return (o instanceof AgentControl);
	}
      };

  protected MobilityFactory mobilityFactory;
  DomainService domain;

  IncrementalSubscription sub;

  public void setDomainService(DomainService domain) {
    this.domain = domain;
    mobilityFactory = 
      (MobilityFactory) domain.getFactory("mobility");
  }
  
  protected void setupSubscriptions() {
    if (mobilityFactory == null) {
      throw new RuntimeException(
				 "Mobility factory (and domain) not enabled");
    }

    Iterator iter = getParameters().iterator();
    String newAgent = (String) iter.next();
    String destNode = (String) iter.next();

    // add the AgentControl request
    addAgent(newAgent, destNode);

    sub = (IncrementalSubscription) blackboard.subscribe(AGENT_CONTROL_PRED);
  }
  
  public void execute() {
    if (sub.hasChanged()) {
      for (Enumeration en = sub.getAddedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("ADDED "+ac);
      }
      for (Enumeration en = sub.getChangedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("CHANGED "+ac);
      }
      for (Enumeration en = sub.getRemovedList(); en.hasMoreElements(); ) {
	AgentControl ac = (AgentControl) en.nextElement();
        System.out.println("REMOVED "+ac);
      }
    }
  }
  
  protected void addAgent(
			  String newAgent,
			  String destNode) {

    MessageAddress newAgentAddr = null;
    MessageAddress destNodeAddr = null;
    if (newAgent != null) {
      newAgentAddr = MessageAddress.getMessageAddress(newAgent);
    }
    if (destNode != null) {
      destNodeAddr = MessageAddress.getMessageAddress(destNode);
    }
    Object ticketId =
      mobilityFactory.createTicketIdentifier();
    AddTicket addTicket = 
      new AddTicket(
		    ticketId,
		    newAgentAddr,
		    destNodeAddr);
  
    AgentControl ac = mobilityFactory.createAgentControl(null, destNodeAddr, addTicket);

    System.out.println("CREATED "+ac);
    blackboard.publishAdd(ac);
  }

} 
