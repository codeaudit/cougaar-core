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
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageTransportClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.mts.AgentState;
import org.cougaar.core.mts.SimpleMessageAttributes;
import org.cougaar.core.service.wp.WhitePagesService;
import org.cougaar.core.service.wp.AddressEntry;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The default, and for now only, implementation of Router.  The
 * <strong>routeMesageMethod</strong> finds the DestinationQueue for
 * each message's target, and enqueues the outgoing message there.  */
final class SingleNodeRouterImpl
{
    static final String VERSION = "version";
    
    private LoggingService loggingService;
    private HashMap agentStates = new HashMap();
    private HashMap clients; 
    private HashMap waitingMsgs; 
    String agentID; 

    public SingleNodeRouterImpl(ServiceBroker sb)
    {
	// talk
	
	
	clients = new HashMap();
	waitingMsgs = new HashMap();
	// agentID = addr.getAddress();
	
	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);
	
// 	WhitePagesService wp = (WhitePagesService)
// 	    sb.getService(this, WhitePagesService.class, null);
// 	long incn = 0;
// 	try {
// 	    AddressEntry entry = wp.get(agentID, VERSION);
// 	    if (entry != null) {
// 		String path = entry.getURI().getPath();
// 		int end = path.indexOf('/', 1);
// 		String incn_str = path.substring(1, end);
// 		incn = Long.parseLong(incn_str);
// 	    }
// 	} catch (Exception ex) {
// 	    if (loggingService.isErrorEnabled())
// 		loggingService.error("Failed Incarnation",ex);
// 	}
// 	incarnation = new Long(incn);
    }
    
    /** Find destination agent's receiving queue then deliver the message to the client**/
    public void routeMessage(Message message) {
	MessageAddress dest = message.getTarget();
	MessageTransportClient dest_client = (MessageTransportClient)
	    clients.get(dest);
	// deliver msgs
	// first check client is registered, if not, hold onto the msg
	if(dest_client == null) {
	    ArrayList messages = (ArrayList) waitingMsgs.get(dest);
	    if (messages == null) {
		messages = new ArrayList();
		waitingMsgs.put(dest, messages);
	    }
	    messages.add(message);
	} else { 
	    deliverMessage(message, dest_client);
	    
	}
    }
    
        
    /** invokes the clients receiveMessage() **/
    public synchronized void deliverMessage(Message message, MessageTransportClient client)
    {
	try {
	    client.receiveMessage(message);
	} catch (Throwable th) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("MessageTransportClient threw an exception in receiveMessage, not retrying.", th);
	}
    }
    
    public void release() {
	// removeAgentState(client.getMessageAddress());	Do we need this??
    }
    
    public void flushMessages(ArrayList droppedMessages) {
    }
    
    public void removeAgentState(MessageAddress id) {
	//agentStates.remove(id.getPrimary());
    }
    
    public boolean okToSend(Message message) {	
	MessageAddress target = message.getTarget();
	if (target == null || target.toString().equals("")) {
	    if (loggingService.isErrorEnabled())
		loggingService.error("Malformed message: "+message);
	    return false;
	} else {
	    return true;
	}
    }
    
    /** Redirects the request to the MessageTransportRegistry. */
    public void registerClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	
	// stick in hashmap
	try {
	    clients.put(key, client);
	} catch (Exception e) {
	    if (loggingService.isErrorEnabled())
		loggingService.error(e.toString());
	}
	
	ArrayList msgs = (ArrayList) waitingMsgs.get(key);
	if (msgs != null) {
	    // look for undelivered msgs & deliver them to newly registerd client
	    for(Iterator iter=msgs.iterator(); iter.hasNext();) {
		Message message = ((Message)iter.next());
		deliverMessage(message, client);
	    }
	}
    }
    
    /**Redirects the request to the MessageTransportRegistry. */
    public void unregisterClient(MessageTransportClient client) {
	MessageAddress key = client.getMessageAddress();
	// remove from client hash
	try {
	    clients.remove(key);
	    waitingMsgs.remove(key);
	} catch (Exception e) {}
    }
    
    public String getIdentifier() {
	return agentID;
    }
    
    /** Redirects the request to the MessageTransportRegistry. */
    public boolean addressKnown(MessageAddress a) {
	return clients.containsKey(a);
    }
 

    public synchronized AgentState getAgentState(MessageAddress id) {
	MessageAddress canonical_id = id.getPrimary();
	Object raw =  agentStates.get(canonical_id);
	if (raw == null) {
	    AgentState state = new SimpleMessageAttributes();
	    agentStates.put(canonical_id, state);
	    return state;
	} else if (raw instanceof AgentState) {
	    return (AgentState) raw;
	} else {
	    throw new RuntimeException("Cached state for " +id+
				       "="  +raw+ 
				       " which is not an AgentState instance");
	}
    }
}
