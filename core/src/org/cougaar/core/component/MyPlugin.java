/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

import org.cougaar.core.cluster.CollectionSubscription;
import org.cougaar.util.UnaryPredicate;

/** Example of how someone might write a basic UltraLog
 * Component similar to a Cougaar plugin.
 **/

public class MyPlugin 
  extends ComponentSupport
{
  private BlackboardBindingSite blackboard;
  private MessageTransportBindingSite messageTransport;

  public void initialize() {
    super.initialize();
    blackboard = (BlackboardBindingSite) getService(BlackboardBindingSite.class);
    messageTransport = (MessageTransportBindingSite) getService(MessageTransportBindingSite.class);
  }

  private Thread runner = null;
  public synchronized void start() {
    runner = new Thread(new Worker());
    runner.start();
  }
  public synchronized void stop() {
    while (runner != null) {
      try {
        runner.interrupt();
        runner.join();
        runner = null;
      } catch (InterruptedException ie) {}
    }
  }

  private class Worker implements Runnable {
    public void run() {
      initializeSubscriptions();
      try {
        while (true) {
          cycle();
          Thread.sleep(1*1000L);
        }
      } catch (InterruptedException ie) {
        finishSubscriptions();
      }
      
    }

    private CollectionSubscription everything = null;
    private void initializeSubscriptions() {
      everything = (CollectionSubscription) blackboard.subscribe(new UnaryPredicate() {
          public boolean execute(Object o) { return true; }
        });
    }
    private void finishSubscriptions() {
      blackboard.unsubscribe(everything);
    }

    private void cycle() {
      // normally, we would do some planning or computation here.  
      // instead, we'll just count the objects in the blackboard.
      System.out.println("MyPlugin sees "+everything.size()+" objects in the Blackboard.");
    }
  }
    
}
