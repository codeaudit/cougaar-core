/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.mts.singlenode;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.core.component.ServiceBroker;

/**
 * Base Single-Node implementation of MessageTransportService.  It
 * does almost nothing by itself - its work is accomplished by
 * redirecting calls to the corresponding registry.  */
public class SingleNodeMTSProxy 
    implements MessageTransportService
{
    private SingleNodeRouterImpl router;
    
    public SingleNodeMTSProxy(SingleNodeRouterImpl router) 
    {
	this.router = router;
    }

    void release() {
	router.release();
	router = null;
    }
    

    /**
     * Checks malformed message, if ok, 
     * -registers message,  
     * -puts in receiving agent's queue.
     */
    public void sendMessage(Message rawMessage) {	
	if (router.okToSend(rawMessage)) router.routeMessage(rawMessage);	
    }
    
    /**
     * Wait for all queued messages for our client to be either
     * delivered or dropped. 
     * @return the list of dropped messages, which could be null.
     */
    public synchronized ArrayList flushMessages() {
	ArrayList droppedMessages = new ArrayList();
	/** No more
	   link.flushMessages(droppedMessages);
	ArrayList rawMessages = new ArrayList(droppedMessages.size());
	Iterator itr = droppedMessages.iterator();
	while (itr.hasNext()) {
	    AttributedMessage m = (AttributedMessage) itr.next();
	    rawMessages.add(m.getRawMessage());
	    }
	
	return rawMessages;
	*/
	return droppedMessages;
    }
    
    

    public AgentState getAgentState() {
	return null;
    }


    /**
     * Adds client to SingleNodeMTSRegistry 
     */
    public synchronized void registerClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	router.registerClient(client);
    }


    /**
     * Redirects the request to the MessageTransportRegistry. */
    public synchronized void unregisterClient(MessageTransportClient client) {
	// Should throw an exception of client != this.client
	router.unregisterClient(client);
    }
    
   
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public String getIdentifier() {
	return router.getIdentifier();
    }
    
    /**
     * Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return router.addressKnown(a);
    }
}

