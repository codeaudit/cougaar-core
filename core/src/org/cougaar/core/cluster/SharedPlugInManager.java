/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.core.cluster;

import java.util.*;

import org.cougaar.core.plugin.PlugInServesCluster;
import org.cougaar.core.plugin.ScheduleablePlugIn;
import org.cougaar.core.cluster.SubscriptionWatcher;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.util.GenericStateModel;

class SharedPlugInManager {
  
  protected ClusterIdentifier cid;
  public SharedPlugInManager(ClusterIdentifier cid) {this.cid = cid;}

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
    // make sure that the scheduler thread is started.  (double check)
    if (schedulerThread == null)
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
    
  /** the thread that the scheduler is running in (if running yet) **/
  private Thread schedulerThread = null;

  synchronized private void assureStarted() {
    if (schedulerThread == null) {
      schedulerThread = new Thread(new ThinScheduler(), "PlugInScheduler/"+cid);
      // don't bother to sync on activitySignal, since 
      // we're protected by the sync on assureStarted().
      schedulerThread.start();
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
                //if (plugin.getState() == GenericStateModel.ACTIVE) {}
                // the waitforsignal should not block, since test() is true.
                try {
                  plugin.externalCycle(watcher.waitForSignal());
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
  }
}

