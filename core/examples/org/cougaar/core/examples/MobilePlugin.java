/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>/** Serve some default prototypes to the system.
 * At this point, this only serves stupid prototypes for
 * (temporary) backward compatability.
 *
 * At start, loads some low-level basics into the registry.
 * On demand, serve a few more.
 **/

 
package org.cougaar.core.examples;

import org.cougaar.core.society.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.plugin.ComponentPlugin;



public class MobilePlugin 
  extends ComponentPlugin
{
    
  public MobilePlugin() {}
  
  protected void setupSubscriptions() {

    final ClusterIdentifier thisAgentID =
      getBindingSite().getAgentIdentifier();

    System.out.println("-----------***INSIDE MOBILE PLUGIN***---------------");
    System.out.println("-------Agent is: " + (thisAgentID.toString()) +"-------");
    // only move if this is 3ID
    if (!("3ID".equals(thisAgentID.toString()))) {
      return;
    }

    // hack to simulate a client - get the message service
    MessageTransportClient mtc = new MessageTransportClient() {
	public void receiveMessage(Message message) {
	  // error!!  shouldn't receive a message on this component level!!
	}
	public MessageAddress getMessageAddress() {   // overload to this agent's address
	  return thisAgentID;
	}
      };
    
    final MessageTransportService mts = (MessageTransportService) 
      getBindingSite().getServiceBroker().getService(
						     mtc,   // simulated client 
						     MessageTransportService.class,
						     null);
    
    if( mts == null) {
      System.out.println("-------MTS is null, be warned!!!-------");
      return;
   }
    
    // get the message service  	
    NodeIdentificationService nodeService = (NodeIdentificationService) 
      getBindingSite().getServiceBroker().getService(
						     this, 
						     NodeIdentificationService.class,
						     null);
    
    
    // no message transport
    if (nodeService == null) {
      //error
      return;
    }
    
    final NodeIdentifier thisNodeID = 
      nodeService.getNodeIdentifier();
     
    // only move if on MiniNode
    if (!("MiniNode".equals(thisNodeID.toString()))) {
      return;
    }
    
    // after 30 seconds move my "3ID" from "MiniNode" to "TestNode"
    final long sleepMS = 10 * 1000;
    final NodeIdentifier toNodeID = new NodeIdentifier("TestNode");
    Runnable moveAgentRunner = new Runnable() {
	public void run() {
	  try {
	    // wait
	    for (long t = 0; t < sleepMS; t += 1000) {
	      System.out.println(
				 "Agent "+thisAgentID+" set to move from "+
				 thisNodeID+" to "+toNodeID+
				 " in "+(sleepMS - t)+" MS");
	      Thread.sleep(1000);
	    }
	    
	    // create the message
	    MoveAgentMessage moveMsg = 
	      new MoveAgentMessage(
				   thisAgentID, // from me
				   thisNodeID,  // tell my node
				   thisAgentID, // to move me
				   toNodeID);   // to destination node
	    
	    System.out.println("Agent "+thisAgentID+" sending "+moveMsg);
	    
	    // send
	    mts.sendMessage(moveMsg);
	  } catch (Exception e) {
	    System.out.println(thisAgentID+" unable to move");
	    e.printStackTrace();
	  }
	}
      };
    Thread moveThread = new Thread(moveAgentRunner);
    moveThread.start();
  }
    
  // no subscriptions, so we'll never actually be run.
  protected void execute() {
  }
}
