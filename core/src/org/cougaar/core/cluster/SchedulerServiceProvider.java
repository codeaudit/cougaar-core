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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import org.cougaar.util.PropertyParser;
import org.cougaar.util.CircularQueue;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.Trigger;
import org.cougaar.core.cluster.ClusterImpl;

/**
 * Scheduler that runs its schedulees in a shared thread
 * The schedulees tell the Scheduler they want to be run via a Trigger.
 * The schedulees pass in a Trigger that the Scheduler calls to activate them.
 * <p>
 * Debugging and behavior parameters: The basic idea is to watch how simple
 * plugins are scheduled: we can watch how long "Shared Thread"
 * plugins take to execute and keep statistics.  We also watch to see
 * if plugins block or otherwise fail to return from execute(). 
 * <p>
 * @property org.cougaar.core.cluster.SchedulerService.statistics=false Set
 * it to true to collect plugin statistics.
 * @property org.cougaar.core.cluster.SchedulerService.dumpStatistics=false Set
 * it to true to get periodic dumps of plugin statistics to the file
 * NODENAME.statistics in the current directory. 
 * @property org.cougaar.core.cluster.SchedulerService.watching=true Set it
 * to false to disable the watcher (default is enabled). When enabled,
 * will complain whever it sees a plugin run or block for more than
 * 15 seconds.  It will also cause the above statistics file(s) to be
 * (re)generated approximately every two minutes.  The watcher is one
 * thread per vm, so it isn't too expensive.
 * @property org.cougaar.core.cluster.SchedulerService.staticScheduler=true
 * When true does shared-thread scheduling over the whole node/vm rather than 
 * per-agent.  Uses the MultiScheduler instead of SimpleScheduler to support
 * multiple worker threads.  Set to false to get the pre-8.6.1 scheduler behavior.
 * @property org.cougaar.core.cluster.SchedulerService.schedulerThreads=4
 * The number of threads to use as workers for the MultiScheduler - these
 * threads are shared among all scheduled components in the node.
 **/
public class SchedulerServiceProvider 
  implements ServiceProvider
{
  /** Should we keep statistics on plugin runtimes? **/
  static boolean keepingStatistics = false;

  /** Should we dump the stats periodically? **/
  static boolean dumpingStatistics = false;

  /** Should we watch for blocked plugins? **/
  static boolean isWatching = true;

  /** how long a plugin runs before we complain when watching (default 2 minutes)**/
  static long warningTime = 120*1000L; 

  /** Should we use a single per-node/vm scheduler (true) or per-agent (false)? **/
  static boolean staticScheduler = true;

  /** How many threads should we use to schedule components when using the MultiScheduler (4) **/
  static int nThreads = 4;

  static {
    String p = "org.cougaar.core.cluster.SchedulerService.";
    keepingStatistics = PropertyParser.getBoolean(p+"statistics", keepingStatistics);
    dumpingStatistics = PropertyParser.getBoolean(p+"dumpStatistics", dumpingStatistics);
    if (dumpingStatistics) keepingStatistics=true;
    isWatching = PropertyParser.getBoolean(p+"watching", isWatching);
    warningTime = PropertyParser.getLong(p+"warningTime", warningTime);
    staticScheduler = PropertyParser.getBoolean(p+"staticScheduler", staticScheduler);
    nThreads = PropertyParser.getInt(p+"schedulerThreads", nThreads);
  }


  private SchedulerBase scheduler;
  private SchedulerService schedulerProxy;

  protected ClusterImpl agent = null;

  public SchedulerServiceProvider() {
    scheduler = createScheduler("Anonymous");
    schedulerProxy = new SchedulerProxy(scheduler);
  }

  public SchedulerServiceProvider(ClusterImpl cluster) {
    scheduler = createScheduler(cluster.toString());
    schedulerProxy = new SchedulerProxy(scheduler);
  }
  

  private static final Object ssLock = new Object();
  private static SchedulerBase singletonScheduler = null;

  protected SchedulerBase createScheduler(String id) {
    if (staticScheduler) {
      synchronized (ssLock) {
        if (singletonScheduler == null) {
          //singletonScheduler = new SimpleScheduler(System.getProperty("org.cougaar.core.society.Node.name", "unknown"));
          singletonScheduler = new MultiScheduler(System.getProperty("org.cougaar.core.society.Node.name", "unknown"));
        }
        return singletonScheduler;
      }
    } else {
      return new SimpleScheduler(id);
    }
  }
    
  // ServiceProvider methods
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    return schedulerProxy;
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service){
  }

  public void suspend() {
    scheduler.suspend();
  }

  public void resume() {
    scheduler.resume();
  }

  static abstract class SchedulerBase implements SchedulerService {
    public Trigger register(Trigger manageMe) {
      assureStarted();
      addClient(manageMe);
      return new SchedulerCallback(manageMe);
    }

    public void unregister(Trigger stopPokingMe) {
      removeClient(stopPokingMe);
    }

    /** Lazy startup of scheduler threads **/
    abstract void assureStarted();

    /** add a client to the schedule list **/
    abstract void addClient(Trigger client);

    /** called to request that client stop being scheduled **/
    abstract void removeClient(Trigger client);

    /** called to request that client be activated asap **/
    abstract void scheduleClient(Trigger client);

    void suspend() {}
    void resume() {}

    /**
     * Components hook into me
     **/
    class SchedulerCallback implements Trigger {
      private Trigger client;
      public SchedulerCallback (Trigger manageMe) {
	client = manageMe;
      }
      /**
       * Add component to the list of pokables to be triggerd
       **/
      public void trigger() {
        scheduleClient(client);
      }
    }

  }

  public static class WorkerBase {
    private long t0 = 0;
    private Trigger currentTrigger = null;
    public boolean runTrigger(Trigger t) {
      try {
        t0 = System.currentTimeMillis();
        currentTrigger = t;
        t.trigger();
      } catch (Throwable die) {
        System.err.println("\nWarning Trigger "+t+" threw "+die);
        die.printStackTrace();
        return true;
      } finally {
        if (keepingStatistics) {
          long delta = System.currentTimeMillis() -t0;
          accumulateStatistics(currentTrigger,delta);
        }
        currentTrigger = null;
        t0 = 0;
      }
      return false;
    }

    public void checkHealth() {
      long tx = t0;
      if (tx>0) {
        long delta = System.currentTimeMillis() -tx;
        if (delta >= warningTime) {
          System.err.println("Warning: Trigger "+currentTrigger+" has been running for "+(delta/1000.0)+" seconds.");
        }
      }
    }
  }


  /** SimpleScheduler is a simple on-demand scheduler of trigger requests.
   * Requests are handled in the order they are requested.
   **/
  static class MultiScheduler
    extends SchedulerBase 
  {
    private final String id;
    MultiScheduler(String id) { this.id = id; }

    private final HashSet clients = new HashSet(13);

    private final CircularQueue runnables = new CircularQueue(32);

    void addClient(Trigger client) {
      synchronized (clients) {
        // only put it on the list if it hasn't already been scheduled
        // Note that this will still allow rescheduling when it is currently
        // being run!  Might be better to have three well-managed states: 
        // idle, pending, running
        // as is, the trigger probably needs to be synchronized to be 
        // safe.
        if (!clients.contains(client)) {
          clients.add(client);
        }
      }
    }
    void removeClient(Trigger client) {
      synchronized (clients) {
        clients.remove(client);
      }
    }

    void scheduleClient(Trigger client) {
      synchronized (runnables) {
        if (true) { // (!runnables.contains(client))
          runnables.add(client);
          runnables.notifyAll();
        }
      }
    }

    /** the scheduler instance (if started) **/
    private boolean running = false;

    private ArrayList threads = null;
    synchronized void assureStarted() {
      if (threads == null) {
        threads = new ArrayList(nThreads);
        for (int i = 0; i<nThreads;i++) {
          Worker scheduler = new Worker(i);
          if (isWatching) {
            getWatcher().register(scheduler);
          }
          String name = "MultiScheduler/"+id+"("+i+")";
          Thread thread = new Thread(scheduler, name);
          running = true;
          thread.start();
          threads.add(thread);
        }
      }
    }

    synchronized void suspend() {
      if (running) {
        // BUG 842: disable MultiScheduler suspend.  This is the low-risk
        // solution until the better fix is ready and well tested.  See 
        // the bug report for further details.
        System.err.println(
            "Warning: Bug 842"+
            " (scheduler \"suspend\" disabled, okay to proceed)");
        /*
        running = false;
        synchronized (runnables) {
          runnables.notifyAll();
        }
        try {
          for (int i =0;i<nThreads;i++) {
            ((Thread)threads.get(i)).join(60000);
          }
        } catch (InterruptedException ie) {
        }
        //schedulerThread = null;
        threads = null;
        */
      }
    }

    synchronized void resume() {
      if (!running) {
        assureStarted();
      }
    }
    class Worker 
      extends WorkerBase
      implements Runnable
    {
      private int id;
      Worker(int i) { id = i; }
      public void run() {
	while (true) {
          Trigger t;
          synchronized (runnables) {
            while (true) {
              if (!(running)) {
                return;
              }
              t = (Trigger) runnables.next();
              if (t != null) {
                break;
              }
              try {
                runnables.wait();
              } catch (InterruptedException ie) {
              }
            }
          }
          runTrigger(t);
	}
      }
    }
  }

  /** SimpleScheduler is a simple on-demand scheduler of trigger requests.
   * Requests are handled in the order they are requested.
   **/
  static class SimpleScheduler
    extends SchedulerBase 
  {
    private final String id;
    SimpleScheduler(String id) { this.id = id; }

    private final HashSet clients = new HashSet(13);
    private final Semaphore sem = new Semaphore();

    private final Object runnableLock = new Object();
    private ArrayList runnables = new ArrayList(13);
    private ArrayList working = new ArrayList(13);

    void addClient(Trigger client) {
      synchronized (clients) {
        clients.add(client);
      }
    }
    void removeClient(Trigger client) {
      synchronized (clients) {
        clients.remove(client);
      }
    }

    void scheduleClient(Trigger client) {
      synchronized (runnableLock) {
        runnables.add(client);
      }
      sem.set();
    }

    /** the scheduler instance (if started) **/
    private Thread schedulerThread = null;
    private boolean running = false;

    synchronized void assureStarted() {
      if (schedulerThread == null) {
	Worker scheduler = new Worker();
        if (isWatching) {
          getWatcher().register(scheduler);
        }
        String name = "SimpleScheduler/"+id;
	schedulerThread = new Thread(scheduler, name);
        running = true;
        schedulerThread.start();
      }
    }

    synchronized void suspend() {
      if (running) {
        running = false;
        sem.set();
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

    class Worker 
      extends WorkerBase
      implements Runnable
    {
      public void run() {
	while (running) {
	  sem.waitForSet();
          synchronized (runnableLock) {
            // swap runnables and working arrays, leaving runnables clear
            ArrayList tmp = runnables;
            runnables = working;
            runnables.clear();
            working = tmp; 
          }
          
          int l = working.size();
	  for (int i = 0; i<l;i++) {
	    Trigger pc = (Trigger)working.get(i);
            runTrigger(pc);
	  }
          working.clear();
	}
      }
    }
  }

  // watcher and statistics support
  
  private static Watcher watcher = null;
  private static synchronized Watcher getWatcher() {
    if (watcher == null) {
      watcher = new Watcher();
      new Thread(watcher, "SchedulerService.Watcher").start();
    }
    return watcher;
  }

  private static class Watcher implements Runnable {
    private long reportTime = 0;

    public void run() {
      while (true) {
        try {
          Thread.sleep(10*1000L); // sleep for a 10 seconds at a time
          long now = System.currentTimeMillis();

          if (isWatching) check();

          // no more often then every two minutes
          if (dumpingStatistics && keepingStatistics && (now-reportTime) >= 120*1000L) {
            reportTime = now;
            report();
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    }

    /** List<WorkerBase> **/
    private ArrayList pims = new ArrayList();
    
    synchronized void register(WorkerBase worker) {
      pims.add(worker);
    }

    /** dump reports on plugin usage **/
    private synchronized void report() {
        
      String nodeName = System.getProperty("org.cougaar.core.society.Node.name", "unknown");
      try {
        File f = new File(nodeName+".statistics");
        FileOutputStream fos = new FileOutputStream(f);
        PrintStream ps = new PrintStream(fos);
        reportStatistics(ps);
        ps.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    /** check the health of each plugin manager, reporting problems **/
    private synchronized void check() {
      for (Iterator i = pims.iterator(); i.hasNext(); ) {
        WorkerBase pim = (WorkerBase) i.next();
        pim.checkHealth();
      }
    }
  }

  // statistics keeper

  private static final HashMap statistics = new HashMap(29);

  static void accumulateStatistics(Trigger trig, long elapsed) {
    synchronized (statistics) {
      InvocationStatistics is = (InvocationStatistics) statistics.get(trig);
      if (is == null) {
        is = new InvocationStatistics(trig);
        statistics.put(trig,is);
      }
      is.accumulate(elapsed);
    }
  }

  public static void reportStatistics(PrintStream os) {
    // the cid should be part of the stats toString
    //os.println(cid.toString());
    synchronized (statistics) {
      for (Iterator i = statistics.values().iterator(); i.hasNext(); ) {
        InvocationStatistics is = (InvocationStatistics) i.next();
        os.println(is.toString());
      }
    }
  }

  public static class InvocationStatistics {
    private int count = 0;
    private long millis = 0L;

    Trigger trigger;
    InvocationStatistics(Trigger p) {
      trigger = p;
    }

    synchronized void accumulate(long elapsed) {
      count++;
      millis+=elapsed;
    }
    public synchronized String toString() {
      double mean = ((millis/count)/1000.0);
      return trigger.toString()+"\t"+count+"\t"+mean;
    }
  }


  // support classes

  /** proxy class to shield the real scheduler from clients **/
  static final class SchedulerProxy implements SchedulerService {
    private final SchedulerService real;
    SchedulerProxy(SchedulerService r) { real = r; }
    public Trigger register(Trigger manageMe) {
      return real.register(manageMe);
    }
    public void unregister(Trigger stopPokingMe) {
      real.unregister(stopPokingMe);
    }
  }    

  public static final class Semaphore {
    public Semaphore() {};

    private boolean attention = false;
    public synchronized boolean isSet() {
      if (attention) {
        attention = false;
        return true;
      } else {
        return false;
      }
    }
    public synchronized void set() {
      attention = true;
      notifyAll();
    }
    public synchronized void waitForSet() {
      while (! attention) {
        try {
          wait();
        } catch (InterruptedException ie) {}
      }
      attention = false;
    }
  }



}
