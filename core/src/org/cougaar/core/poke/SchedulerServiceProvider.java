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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.cougaar.core.component.*;

public class SchedulerServiceProvider // Should this be called SchedulerServiceProvider?
  implements SchedulerService, ServiceProvider
{

  private HashMap clients = new HashMap(13);
  private HashSet runThese = new HashSet(13);

  public SchedulerServiceProvider() {}

  
  // ServiceProvider methods
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    // this seems wrong
    return this;
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service){
  }


  public Pokable register(Pokable component) {
    assureStarted();
    return new SchedulerCallback(component);
  }

  public void unregister(Pokable component) {
    clients.remove(component);
  }

  public void setPokable(Component comp, Pokable pc) {
    // stuff this in a hashtable.
    clients.put(comp, pc);

//      synchronized(runThese) {
//        runThese.add(comp);
//      }
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
	synchronized(runThese) {
	  for (Iterator it = runThese.iterator(); it.hasNext();) {
	    Pokable pc = (Pokable)it.next();
	    pc.poke();
	    it.remove();
	  }
	}
      }
    }
  }

  /**
   * Components hook into me
   **/
  protected class SchedulerCallback implements Pokable {
    private Pokable component = null;
    public SchedulerCallback (Pokable comp) {
      component = comp;
    }
    public void poke() {
      System.out.println("SchedulerServiceProvider.SchedulerCallback.poke() - ouch! I've been poked");
      synchronized(runThese) {
	runThese.add(component);
	signalActivity();
      }
    }
  }

}
