/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.UIDServer;
import org.cougaar.core.cluster.SubscriptionWatcher;

/** The standard Binder for Plugins.
 **/
public class PluginBinder extends BinderSupport implements PluginBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public PluginBinder(Object parentInterface, Component child) {
    super(((PluginManager) parentInterface).getChildContext(), 
          (PluginManager) parentInterface, 
          child);
  }

  /** package-private kickstart method for use by the PluginBinderFactory **/
  void initialize() {
    initializeChild();          // set up initial services
    // start the right threading model.
    setThreadingModel(createThreadingModel());

  }

  protected final PluginBase getPlugin() {
    return (PluginBase) getChildComponent();
  }
  protected final PluginManager getPluginManager() {
    return (PluginManager)getParentComponent();
  }


  //
  // cluster  
  // 

  public final ClusterIdentifier getAgentIdentifier() {
    return getPluginManager().getAgentIdentifier();
  }

  public final UIDServer getUIDServer() {
    return getPluginManager().getUIDServer();
  }

  public ConfigFinder getConfigFinder() {
    return getPluginManager().getConfigFinder();
  }


  private Threading threadingModel = null;
  
  protected final void setThreadingModel(Threading t) {
    threadingModel = t;
  }

  protected final Threading getThreadingModel() { 
    return threadingModel;
  }
  
  public final static int UNSPECIFIED_THREAD = -1;
  public final static int NO_THREAD = 0;
  public final static int SHARED_THREAD = 1;
  public final static int SINGLE_THREAD = 2;
  public final static int ONESHOT_THREAD = 3;

  private int threadingChoice = UNSPECIFIED_THREAD;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  public final void setThreadingChoice(int m) {
    if (threadingModel != null) 
      throw new IllegalArgumentException("Too late to select threading model.");
    threadingChoice = m;
  }

  /** @return the current choice of threading model.  **/
  public final int getThreadingChoice() {
    return threadingChoice;
  }

  /** create a Threading model object as specified by the plugin.
   * The default implementation creates a Threading object
   * based on the value of threadingChoice.
   * The default choice is to use a SharedThreading model, which
   * shares thread of execution with others of the same sort in
   * the cluster.
   * Most plugins can ignore this altogether.  Most that
   * want to select different behavior should
   * call chooseThreadingModel() in their constructer.
   * PlugIns which implement their own threading model
   * will need to override createThreadingModel.
   * createThreadingModel is called late in PluginBinder.load(). 
   * if an extending plugin class wishes to examine or alter
   * the threading model object, it will be available only when 
   * PluginBinder.load() returns, which is usually called by
   * the extending plugin classes overriding load() method.
   * The constructed Threading object is initialized by
   * PluginBinder.start().
   **/
  protected Threading createThreadingModel() {
    Threading t;
    switch (getThreadingChoice()) {
    case NO_THREAD:
      t = new NoThreading();
      break;
    case SHARED_THREAD: 
      t = new SharedThreading();
      break;
    case SINGLE_THREAD:
      t = new SingleThreading();
      break;
    case ONESHOT_THREAD:
      t = new OneShotThreading();
      break;
    default:
      throw new RuntimeException("Invalid Threading model "+getThreadingChoice());
    }
    return t;
  }

  public void startThreadingModel() {
    try {
      threadingModel.initialize();
      threadingModel.load(null);
      threadingModel.start();
    } catch (RuntimeException e) {
      System.err.println("Caught exception during threadingModel initialization: "+e);
      e.printStackTrace();
    }
  }

  protected abstract class Threading implements GenericStateModel {
    public void initialize() {}
    /** the argument passed to load is a ClusterServesPlugIn **/
    public void load(Object o) {}
    public void start() {}
    public void suspend() {}
    public void resume() {}
    public void stop() {}
    public void halt() {}
    public void unload() {}
    public int getState() { 
      return UNINITIALIZED; 
    }
    public String toString() {
      return getAgentIdentifier()+"/"+(PluginBinder.this);
    }
  }

  /** up to the class to implement what it needs **/
  protected class NoThreading extends Threading {
  }
    
  /** prerun only: cycle will never be called. **/
  protected class OneShotThreading extends Threading {
    public OneShotThreading() {}
    public void start() {
      plugin_prerun();
    }
  }

  /** shares a Thread with other SharedThreading plugins in the same cluster **/
  protected class SharedThreading extends Threading implements ScheduleablePlugIn {
    public SharedThreading() {}
    public void start() {
      //getPluginManager().schedulePlugIn(this);
      plugin_prerun();
    }

    //
    // implementation of ScheduleablePlugIn API 
    //

    public void addExternalActivityWatcher(SubscriptionWatcher watcher) { 
      // what the heck do I do with this? Is this Beth's deal?
      //(getPlugInAdapter().getBlackboardService()).registerInterest(watcher);
      //System.out.println("PluginBinder.addExternalActivityWatcher()");
    }

    public final void externalCycle(boolean wasExplicit) {
      // ???
      getPlugin().setAwakened(wasExplicit);
      plugin_cycle();
    }
  }

  /** has its own Thread **/
  protected class SingleThreading extends Threading implements Runnable {
    /** a reference to personal Thread which each PlugIn runs in **/
    private Thread myThread = null;
    /** our subscription watcher **/
    private SubscriptionWatcher waker = null;
    
    public SingleThreading() {}

    private int priority = Thread.NORM_PRIORITY;

    /** plugins and subclasses may set the Thread priority to 
     * a value lower than standard.  Requests to raise the priority
     * are ignored as are all requests after start()
     * Note that the default priority is one level below the
     * usual java priority - that is one level below where the
     * infrastructure runs.
     **/
    public void setPriority(int newPriority) {
      if (newPriority<priority) {
        priority = newPriority;
      }
    }
    
    private boolean isYielding = true;

    /** If isYielding is true, the plugin will force a thread yield
     * after each call to cycle().  This is on by default since plugins
     * generally need reaction from infrastructure and other plugins
     * to progress.
     * This may be set at any time, even though the effect is only periodic.
     * Most plugins would want to (re)set this value at initialization.
     **/
    public void setIsYielding(boolean v) {
      isYielding = v;
    }

    public void load(Object object) {
    }
    public void start() {
      myThread = new Thread(this, "Plugin/"+getAgentIdentifier()+"/"+(PluginBinder.this));
      myThread.setPriority(priority);
      myThread.start();
    }

    private boolean suspendRequest = false;
    public void suspend() { 
      if (myThread != null) {
        suspendRequest = true;
        signalStateChange();
      }
    }

    private boolean resumeRequest = false;
    public void resume() {  
      if (myThread != null) {
        resumeRequest = true;
        signalStateChange();
      }
    }
    private boolean stopRequest = false;
    public void stop() {
      if (myThread != null) {
        stopRequest = true;
        signalStateChange();
      }
    }

    private void signalStateChange() {
      if (waker != null) {
        waker.signalNotify(waker.INTERNAL);
      }
    }

    private boolean isRunning = true;
    private boolean isActive = true;
    public final void run() {
      plugin_prerun();                 // plugin first time through
      while (isRunning) {
	boolean xwakep = waker.waitForSignal();
	getPlugin().setAwakened(xwakep);
        if (suspendRequest) {
          suspendRequest = false;
          isActive = false;
        }
        if (resumeRequest) {
          resumeRequest = false;
          isActive = true;
        }
        if (stopRequest) {
          stopRequest = false;
          isRunning = false;
          isActive = false;
        }
        if (isActive) {
          plugin_cycle();                // do work
          if (isYielding)
            Thread.yield();
        }
      }
    }
    public void setWaker(SubscriptionWatcher sw) {
      waker = sw;
    }
  }

  /** Called by all the standard Threading models to instruct the plugin 
   * to do plugin-specific initializations.  E.g. setup its subscriptions, etc.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  protected void plugin_prerun() {
    getPlugin().start(); // just in case..
    getPlugin().plugin_prerun();
  }

  /** Called by all the standard Threading models (except for OneShotThreading)
   * each time there is work to be done.
   *
   * Non-standard threading models are encouraged but not required to use 
   * this method to retain compatability.
   **/
  protected void plugin_cycle() {
    getPlugin().plugin_cycle();
  }


}
