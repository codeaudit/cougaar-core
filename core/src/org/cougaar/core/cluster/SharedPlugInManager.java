/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.plugin.ScheduleablePlugIn;
import org.cougaar.core.cluster.SubscriptionWatcher;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.util.GenericStateModel;
import org.cougaar.util.PropertyParser;

import java.util.*;
import java.io.*;

/** Manager (really a Scheduler) for lightweight plugins.
 * <p>
 * Debugging and behavior parameters: The basic idea is to watch how simple
 * plugins are scheduled: we can watch how long "Shared Thread"
 * plugins take to execute and keep statistics.  We also watch to see
 * if plugins block or otherwise fail to return from execute(). 
 * <p>
 * org.cougaar.core.cluster.SharedPlugInManager.statistics=false Set
 * it to true to collect plugin statistics.
 * <p>
 * org.cougaar.core.cluster.SharedPlugInManager.dumpStatistics=false Set
 * it to true to get periodic dumps of plugin statistics to the file
 * NODENAME.statistics in the current directory. 
 * <p>
 * org.cougaar.core.cluster.SharedPlugInManager.watching=true Set it
 * to false to disable the watcher (default is enabled). When enabled,
 * will complain whever it sees a plugin run or block for more than
 * 15 seconds.  It will also cause the above statistics file(s) to be
 * (re)generated approximately every two minutes.  The watcher is one
 * thread per vm, so it isn't too expensive.
 * <p>
 **/
class SharedPlugInManager implements SharedThreadingService {
  
  /** Should we keep statistics on plugin runtimes? **/
  static boolean keepingStatistics = false;

  /** Should we dump the stats periodically? **/
  static boolean dumpingStatistics = false;

  /** Should we watch for blocked plugins? **/
  static boolean isWatching = true;

  /** how long a plugin runs before we complain when watching **/
  static long warningTime = 120*1000L; 

  static {
    String p = "org.cougaar.core.cluster.SharedPlugInManager.";
    keepingStatistics = PropertyParser.getBoolean(p+"statistics", keepingStatistics);
    dumpingStatistics = PropertyParser.getBoolean(p+"dumpStatistics", dumpingStatistics);
    if (dumpingStatistics) keepingStatistics=true;
    isWatching = PropertyParser.getBoolean(p+"watching", isWatching);
    warningTime = PropertyParser.getLong(p+"warningTime", warningTime);
  }

  protected ClusterIdentifier cid;

  public SharedPlugInManager(ClusterIdentifier cid) {
    this.cid = cid;
    if (isWatching || dumpingStatistics)
      getWatcher().register(this);
  }

  /** map of plugin->watcher.  
   * Also used as the lock protecting itself, pq, and pqBack
   **/
  private HashMap plugins = new HashMap(13);
  /** set of plugins to consider on next execution **/
  private ArrayList pq = new ArrayList(13);
  /** double buffer back for pq **/
  private ArrayList pqBack = new ArrayList(13);

  public int size() { return pq.size(); }
  public String toString() { 
    return "SPM("+cid.toString()+","+pq.size()+")";
  }


  /** called by cluster after plugin is loaded, but before it is
   * started.
   **/
  public void registerPlugIn(ScheduleablePlugIn plugin) {
    assureStarted();

    SubscriptionWatcher watcher = new ThinWatcher(plugin);
    synchronized (plugins) {    // make sure not to mod while traversing
      plugins.put(plugin, watcher);
      pq.add(watcher);
      pqBack.add(null);         // add a bucket for the backing store

      // tell the plugin's subscriber that we need to be notified of
      // updates.
      plugin.addExternalActivityWatcher(watcher);
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
    
  /** delegate health checks to the scheduler **/
  synchronized void checkHealth() {
    if (scheduler != null)
      scheduler.checkHealth();
  }

  /** the scheduler instance (if started) **/
  private ThinScheduler scheduler = null;

  synchronized private void assureStarted() {
    if (scheduler == null) {
      scheduler = new ThinScheduler();
      (new Thread(scheduler, "PlugInScheduler/"+cid)).start();
    }
  }

  protected class ThinWatcher extends SubscriptionWatcher {
    // keep a link to our plugin to avoid multiple hashes.
    private ScheduleablePlugIn plugin;
    public ThinWatcher(ScheduleablePlugIn plugin) { this.plugin = plugin; }
    public ScheduleablePlugIn getPlugIn() { return plugin; }

    public boolean needsAttention() { return test(); }
    public void signalNotify(int event) {
      super.signalNotify(event);
      signalActivity();
    }
  }

  protected class ThinScheduler implements Runnable {
    public void run() {
      while (true) {
        waitForActivity();

        // lock on the hashtable
        synchronized (plugins) {
          // loop over the the front queue
          int l = pq.size();
          int j=l;              // where to put executed ones in pqBack
          for (int i = 0; i<l; i++) {
            ThinWatcher watcher = (ThinWatcher) pq.get(i);
            if (watcher != null) {
              if ( watcher.needsAttention()) {
                ScheduleablePlugIn plugin = watcher.getPlugIn();
                try {
                  runPlugin(plugin, watcher);
                } catch (Throwable die) {
                  System.err.println("\nPlugin "+plugin+"("+cid+") raised "+die);
                  die.printStackTrace();
                  // should probably remove it from the cycle...
                }

                //move to pqBack
                j--;
                pqBack.set(j, watcher);
                pq.set(i,null);
              }
            }
          }

          // now plugins which ran are at the end of pqBack, but in wrong order.
          // we'll reorder them now...
          int k = (l-1);
          for (int i=j; i<k; i++, k--) {
            Object tmp = pqBack.get(i);
            pqBack.set(i, pqBack.get(k));
            pqBack.set(k, tmp);
          }
          // move the rest of pq to the front of pqBack, clearing as we go
          j = 0;
          for (int i=0; i<l; i++) {
            Object tmp = pq.get(i);
            if (tmp != null) {
              pqBack.set(j, tmp);
              j++;
              pq.set(i, null);
            }
          }

          // swap pq and pqBack
          ArrayList tmp = pqBack;
          pqBack = pq;
          pq = tmp;

          //System.err.println("\t"+(SharedPlugInManager.this)+" counted "+c);
        }
      }
    }

    private ScheduleablePlugIn currentPlugin = null;
    private long t0 = 0; 

    private void runPlugin(ScheduleablePlugIn plugin, ThinWatcher watcher) {
      //if (plugin.getState() == GenericStateModel.ACTIVE) {}

      synchronized (this) {
        currentPlugin = plugin;
        t0 = System.currentTimeMillis();
      }

      // the waitforsignal should not block, since test() is true.
      try {
        plugin.externalCycle(watcher.waitForSignal());

        if (keepingStatistics)
          accumulate(plugin, System.currentTimeMillis()-t0);
      } finally {               // make sure not to mis-record a thrown plugin
        synchronized (this) {
          currentPlugin = null;
        }
      }
    }

    void checkHealth() {
      synchronized (this) {
        if (currentPlugin != null) {
          long delta = System.currentTimeMillis() - t0;
          if (delta >= warningTime) {
            System.err.println("Warning "+cid+" "+currentPlugin+" has been running for "+(delta/1000.0)+" seconds");
          }
        }
      }
    }
  }

  // statistics keeper

  private HashMap statistics = new HashMap(29);

  private synchronized void accumulate(ScheduleablePlugIn plugin, long elapsed) {
    InvocationStatistics is = (InvocationStatistics) statistics.get(plugin);
    if (is == null) {
      is = new InvocationStatistics(plugin);
      statistics.put(plugin,is);
    }
    is.accumulate(elapsed);
  }

  static class InvocationStatistics {
    private int count = 0;
    private long millis = 0L;

    ScheduleablePlugIn plugin;
    InvocationStatistics(ScheduleablePlugIn p) {
      plugin = p;
    }

    synchronized void accumulate(long elapsed) {
      count++;
      millis+=elapsed;
    }
    public synchronized String toString() {
      double mean = ((millis/count)/1000.0);
      return plugin.toString()+"\t"+count+"\t"+mean;
    }
  }

  synchronized void reportStatistics(PrintStream os) {
    // the cid should be part of the stats toString
    //os.println(cid.toString());
    for (Iterator i = statistics.values().iterator(); i.hasNext(); ) {
      InvocationStatistics is = (InvocationStatistics) i.next();
      os.println(is.toString());
    }
  }


  // watcher
  
  private static Watcher watcher = null;

  private static synchronized Watcher getWatcher() {
    if (watcher == null) {
      watcher = new Watcher();
      new Thread(watcher, "SharedPlugInManager Watcher").start();
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

    /** List<SharedPlugInManager> **/
    private ArrayList pims = new ArrayList();
    
    synchronized void register(SharedPlugInManager pim) {
      pims.add(pim);
    }

    /** dump reports on plugin usage **/
    private synchronized void report() {
        
      String nodeName = System.getProperty("org.cougaar.core.society.Node.name", "unknown");
      try {
        File f = new File(nodeName+".statistics");
        FileOutputStream fos = new FileOutputStream(f);
        PrintStream ps = new PrintStream(fos);
        for (Iterator i = pims.iterator(); i.hasNext(); ) {
          SharedPlugInManager pim = (SharedPlugInManager) i.next();
          pim.reportStatistics(ps);
        }
        ps.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    /** check the health of each plugin manager, reporting problems **/
    private synchronized void check() {
      for (Iterator i = pims.iterator(); i.hasNext(); ) {
        SharedPlugInManager pim = (SharedPlugInManager) i.next();
        pim.checkHealth();
      }
    }
  }

}

