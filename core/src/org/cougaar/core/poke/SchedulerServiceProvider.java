/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.poke;

import org.cougaar.core.component.ServiceProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.core.component.*;

/**
 * Scheduler that runs its schedulees in a shared thread
 * The schedulees tell the Scheduler they want to be run via a Pokable.
 * The schedulees pass in a Pokable that the Scheduler calls to activate them.
 */
public class SchedulerServiceProvider 
  implements ServiceProvider
{

  private HashSet clients = new HashSet(13);
  private ArrayList runThese = new ArrayList(13);
  private SchedulerServiceImpl scheduler = new SchedulerServiceImpl();
  private Object runListSemaphore = new Object();


  public SchedulerServiceProvider() {}
  
  
  // ServiceProvider methods
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return scheduler;
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service){
  }


  protected class SchedulerServiceImpl implements SchedulerService{
    // Poker interface methods
    public Pokable register(Pokable manageMe) {
      assureStarted();
      clients.add(manageMe);
      return new SchedulerCallback(manageMe);
    }

    public void unregister(Pokable stopPokingMe) {
      clients.remove(stopPokingMe);
    }

    /** the scheduler instance (if started) **/
    private EasyScheduler scheduler = null;

    synchronized private void assureStarted() {
      if (scheduler == null) {
	scheduler = new EasyScheduler();
	(new Thread(scheduler, "SchedulerServiceProvider/"/*+cid*/)).start();
      }
    }

    /** Semaphore to signal activity in one or more plugins **/
    private Object activitySignal = new Object();
    
    /** flag to simplify synchronization on startup **/
    private boolean activitySignaled = false;
    
    protected void signalActivity() {
      if (activitySignaled) return; // reduce lock contention
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
	while (true) {
	  waitForActivity();
	  //make copy to prevent concurrent modification error. (I couldn't figure out the synchronization)
	  ArrayList pokables;
	  synchronized(runListSemaphore) {
	    pokables = new ArrayList(runThese);
	    runThese.clear();
	  }
	  for (Iterator it = pokables.iterator(); it.hasNext();) {
	    Pokable pc = (Pokable)it.next();
	    pc.poke();
	  }
	}
      }
    }


    /**
     * Components hook into me
     **/
    protected class SchedulerCallback implements Pokable {
      private Pokable componentsPokable = null;
      public SchedulerCallback (Pokable manageMe) {
	componentsPokable = manageMe;
      }
      /**
       * Add component to the list of pokables to be poked
       **/
      public void poke() {
	System.out.println("SchedulerServiceProvider.SchedulerCallback.poke() - ouch! I've been poked");
	synchronized(runListSemaphore) {
	  runThese.add(componentsPokable);
	  signalActivity();
	}
      }
    }
  }
}
