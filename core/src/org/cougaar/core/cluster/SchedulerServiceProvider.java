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

package org.cougaar.core.cluster;

import org.cougaar.core.component.ServiceProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.Trigger;
import org.cougaar.core.cluster.ClusterImpl;

/**
 * Scheduler that runs its schedulees in a shared thread
 * The schedulees tell the Scheduler they want to be run via a Trigger.
 * The schedulees pass in a Trigger that the Scheduler calls to activate them.
 */
public class SchedulerServiceProvider 
  implements ServiceProvider
{

  private HashSet clients = new HashSet(13);
  private ArrayList runThese = new ArrayList(13);
  private SchedulerServiceImpl scheduler = new SchedulerServiceImpl();
  private Object runListSemaphore = new Object();
  protected ClusterImpl agent = null;

  public SchedulerServiceProvider() {}

  public SchedulerServiceProvider(ClusterImpl cluster) {
  agent = cluster;
  }
  
  
  // ServiceProvider methods
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return scheduler;
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service){
  }

  public void suspend() {
    scheduler.suspend();
  }

  public void resume() {
    scheduler.resume();
  }

  protected class SchedulerServiceImpl implements SchedulerService{

    public Trigger register(Trigger manageMe) {
      assureStarted();
      clients.add(manageMe);
      return new SchedulerCallback(manageMe);
    }

    public void unregister(Trigger stopPokingMe) {
      clients.remove(stopPokingMe);
    }

    /** the scheduler instance (if started) **/
    private Thread schedulerThread = null;
    private boolean running = false;

    synchronized private void assureStarted() {
      if (schedulerThread == null) {
	Runnable scheduler = new EasyScheduler();
        String name = "SchedulerServiceProvider/" + agent.getClusterIdentifier();
	schedulerThread = new Thread(scheduler, name);
        running = true;
        schedulerThread.start();
      }
    }

    synchronized void suspend() {
      if (running) {
        running = false;
        signalActivity();
        try {
          schedulerThread.join(60000);
        } catch (InterruptedException ie) {
        }
        schedulerThread = null;
      }
    }

    synchronized void resume() {
      if (!running) {
        assureStarted();
      }
    }

    /** Semaphore to signal activity in one or more plugins **/
    private Object activitySignal = new Object();
    
    /** flag to simplify synchronization on startup **/
    private boolean activitySignaled = false;
    
    protected void signalActivity() {
      synchronized (activitySignal) {
	if (!activitySignaled) {  // reduce lock,notify contention
	  activitySignaled = true;
	  // only one consumer - no need for notifyAll, though
	  // we could support multiple worker threads (neato!)
	  activitySignal.notify();
	}
      }
    }

    /** Block until there is (or was) activity. 
     */
    protected void waitForActivity() {
      synchronized (activitySignal) {
	while (! activitySignaled ) {
	  try {
	    activitySignal.wait();
	  } catch (InterruptedException ie) {}
	}
	activitySignaled = false;
      }
    }

    protected class EasyScheduler implements Runnable {
      public void run() {
	while (running) {
	  waitForActivity();
	  //make copy to prevent concurrent modification error. (I couldn't figure out the synchronization)
	  ArrayList pokables;
	  synchronized(runListSemaphore) {
	    pokables = new ArrayList(runThese);
	    runThese.clear();
	  }
	  for (Iterator it = pokables.iterator(); running && it.hasNext();) {
	    Trigger pc = (Trigger)it.next();
	    pc.trigger();
	  }
	}
      }
    }


    /**
     * Components hook into me
     **/
    protected class SchedulerCallback implements Trigger {
      private Trigger componentsTrigger = null;
      public SchedulerCallback (Trigger manageMe) {
	componentsTrigger = manageMe;
      }
      /**
       * Add component to the list of pokables to be triggerd
       **/
      public void trigger() {
	//System.out.println("SchedulerServiceProvider.SchedulerCallback.trigger() - ouch! I've been triggerd");
	synchronized(runListSemaphore) {
	  runThese.add(componentsTrigger);
	  signalActivity();
	}
      }
    }
  }
}
