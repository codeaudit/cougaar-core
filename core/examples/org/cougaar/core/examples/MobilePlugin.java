/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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
 **/

package org.cougaar.core.examples;

import java.util.*;

import org.cougaar.core.society.*;
import org.cougaar.core.cluster.*;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.util.UnaryPredicate;

/**
 * Example Plugin that requests it's Agent to move to another Node.
 */
public class MobilePlugin 
  extends ComponentPlugin
{
    
  public MobilePlugin() {
  }
  
  protected void setupSubscriptions() {

    // parse the parameters
    String paramOrigNode;
    String paramDestNode;
    final int paramSleepSeconds;
    try {
      List params = (List)getParameters();
      paramOrigNode = (String)params.get(0);
      paramDestNode = (String)params.get(1);
      paramSleepSeconds = Integer.parseInt((String)params.get(2));
    } catch (Exception e) {
      System.err.println(
          "Expecting three parameters to "+getClass().getName()+
          ": (origNode, destNode, sleepSeconds)\n"+
          e);
      return;
    }

    // get our agent's ID
    final ClusterIdentifier thisAgentID =
      getBindingSite().getAgentIdentifier();

    // parse the destination node ID
    final NodeIdentifier toNodeID = new NodeIdentifier(paramDestNode);

    // get the nodeID service          
    NodeIdentificationService nodeService = (NodeIdentificationService) 
      getBindingSite().getServiceBroker().getService(
          this, 
          NodeIdentificationService.class,
          null);
    final NodeIdentifier thisNodeID = 
      ((nodeService != null) ? 
       nodeService.getNodeIdentifier() :
       null);

    // only move if on the correct Node
    if ((thisNodeID == null) ||
        (!(paramOrigNode.equals(thisNodeID.toString())))) {
      System.err.println(
          "Not moving, since Agent "+thisAgentID+" is on Node "+
          thisNodeID+", not "+paramOrigNode);

      // see if a TestObject is in the Blackboard
      UnaryPredicate pred = 
        new UnaryPredicate() {
          public boolean execute(Object o) {
            return (o instanceof TestObject);
          }
        };
      Collection c = blackboard.query(pred);
      int n = ((c != null) ? c.size() : 0);
      if (n > 0) {
        Iterator iter = c.iterator();
        for (int i = 0; i < n; i++) {
          System.out.println(
              "Found TestObject["+i+" / "+n+"]: "+
              iter.next());
        }
      } else {
        System.out.println("No TestObjects found");
      }

      return;
    }

    // put something in the Blackboard to see if it moves with us
    TestObject tObj = new TestObject(thisNodeID, thisAgentID);
    System.out.println(
        "Agent "+thisAgentID+" on Node "+thisNodeID+
        " publishing a TestObject: "+tObj);
    blackboard.publishAdd(tObj);

    // create a dummy message transport client
    MessageTransportClient mtc = 
      new MessageTransportClient() {
        public void receiveMessage(Message message) {
          // never
        }
        public MessageAddress getMessageAddress() {
          return thisAgentID;
        }
      };

    // get the message transport
    final MessageTransportService mts = (MessageTransportService) 
      getBindingSite().getServiceBroker().getService(
          mtc,   // simulated client 
          MessageTransportService.class,
          null);
    if (mts == null) {
      System.out.println(
          "Unable to get message transport service");
      return;
    }

    // create a runner to do our work
    Runnable moveAgentRunner = new Runnable() {
      public void run() {
        try {
          // sleep a while
          for (int t = paramSleepSeconds; t > 0; t--) {
            System.out.println(
                "Move Agent "+thisAgentID+" from "+
                thisNodeID+" to "+toNodeID+
                " in T-MINUS "+t+" seconds");
            Thread.sleep(1000);
          }

          // create the move-message
          MoveAgentMessage moveMsg = 
            new MoveAgentMessage(
                thisAgentID, // from me
                thisNodeID,  // tell my node
                thisAgentID, // to move me
                toNodeID);   // to destination node

          System.out.println("Agent "+thisAgentID+" sending "+moveMsg);

          // send
          mts.sendMessage(moveMsg);

          // hopefully it'll happen...
        } catch (Exception e) {
          System.out.println(
              "Agent "+thisAgentID+" on Node "+
              thisNodeID+" unable to move");
          e.printStackTrace();
        }
      }
    };
    Thread moveThread = new Thread(moveAgentRunner);
    moveThread.start();

    // let the thread run..
  }

  // no subscriptions, so we'll never actually be run.
  protected void execute() {
  }

  /**
   * Simple test object for the Blackboard.
   * <p>
   * Should probably make this a <code>UniqueObject</code>
   * and <code>XMLable</code> for the UI.
   */
  private static class TestObject implements java.io.Serializable {
    private String s;
    public TestObject(
        NodeIdentifier nid,
        ClusterIdentifier aid) {
      s = "{Node "+nid+" Agent "+aid+"}";
    }
    public String toString() {
      return s;
    }
  }
}
