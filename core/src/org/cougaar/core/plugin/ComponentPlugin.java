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

package org.cougaar.core.plugin;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.blackboard.SubscriptionWatcher;

import org.cougaar.core.agent.ClusterServesPlugin;
import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Trigger;
import org.cougaar.util.TriggerModel;
import org.cougaar.util.SyncTriggerModelImpl;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

/**
 * Standard base-class for Components that watch the Blackboard for 
 * activity and use the shared thread-scheduler.
 * <p>
 * Create a derived class by implementing 
 * <tt>setupSubscriptions()</tt> and <tt>execute()</tt>.
 * <p>
 * Note that both "precycle()" and "cycle()" will be run by the
 * scheduler.  This means that the scheduling order <i>in relation to 
 * other scheduled Components</i> may be *random* (i.e. this 
 * ComponentPlugin might load first but be precycled last!).  In 
 * general a Component should <b>not</b> make assumptions about the 
 * load or schedule ordering.
 */
public abstract class ComponentPlugin 
  extends org.cougaar.util.GenericStateModelAdapter
  implements PluginBase, BlackboardClient 
{
  
  private Collection parameters;

  private SchedulerService scheduler;
  protected BlackboardService blackboard;
  protected AlarmService alarmService;

  protected String blackboardClientName;

  private PluginBindingSite pluginBindingSite;
  private ServiceBroker serviceBroker;

  private TriggerModel tm;
  private SubscriptionWatcher watcher;
  
  public ComponentPlugin() { 
  }
  
  /**
   * Called just after construction (via introspection) by the 
   * loader if a non-null parameter Object was specified by
   * the ComponentDescription.
   **/
  public void setParameter(Object param) {
    if (param != null) {
      parameters = (Vector) param;
    } else {
      parameters = new Vector(0);
    }
  }
  
  /** 
   * Get any Plugin parameters passed by the plugin instantiator.
   */
  public Collection getParameters() {        
    return parameters;
  }
  
  /**
   * Binding site is set by reflection at creation-time.
   */
  public void setBindingSite(BindingSite bs) {
    if (bs instanceof PluginBindingSite) {
      pluginBindingSite = (PluginBindingSite)bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
    serviceBroker = pluginBindingSite.getServiceBroker();
  }

  /**
   * Get the binding site, for subclass use.
   */
  protected PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }
  
  /** 
   * Get the binding site, for subclass use.
   */
  protected ServiceBroker getServiceBroker() {
    return serviceBroker;
  }

  // rely upon load-time introspection to set these services - 
  //   don't worry about revokation.
  public final void setSchedulerService(SchedulerService ss) {
    scheduler = ss;
  }
  public final void setBlackboardService(BlackboardService bs) {
    blackboard = bs;
  }
  public final void setAlarmService(AlarmService s) {
    alarmService = s;
  }

  /**
   * Get the blackboard service, for subclass use.
   */
  protected BlackboardService getBlackboardService() {
    return blackboard;
  }

  /**
   * Get the alarm service, for subclass use.
   */
  protected AlarmService getAlarmService() {
    return alarmService;
  }
  
  //
  // implement GenericStateModel:
  //

  public void initialize() {
    super.initialize();
  }

  public void load() {
    super.load();
    
    // create a blackboard watcher
    this.watcher = 
      new SubscriptionWatcher() {
        public void signalNotify(int event) {
          // gets called frequently as the blackboard objects change
          super.signalNotify(event);
          tm.trigger();
        }
        public String toString() {
          return "ThinWatcher("+ComponentPlugin.this.toString()+")";
        }
      };

    // create a callback for running this component
    Trigger myTrigger = 
      new Trigger() {
        private boolean didPrecycle = false;
        // no need to "sync" when using "SyncTriggerModel"
        public void trigger() {
          awakened = watcher.clearSignal();
          try {
            if (didPrecycle) {
              cycle();
            } else {
              didPrecycle = true;
              precycle();
            }
          } finally {
            awakened = false;
          }
        }
        public String toString() {
          return "Trigger("+ComponentPlugin.this.toString()+")";
        }
      };

    // create the trigger model
    this.tm = new SyncTriggerModelImpl(scheduler, myTrigger);

    // activate the blackboard watcher
    blackboard.registerInterest(watcher);

    // activate the trigger model
    tm.initialize();
    tm.load();
  }

  public void start() {
    super.start();
    tm.start();
    // Tell the scheduler to run me at least this once
    tm.trigger();
  }

  public void suspend() {
    super.suspend();
    tm.suspend();
  }

  public void resume() {
    super.resume();
    tm.resume();
  }

  public void stop() {
    super.stop();
    tm.stop();
  }

  public void halt() {
    super.halt();
    tm.halt();
  }
  
  public void unload() {
    super.unload();
    if (tm != null) {
      tm.unload();
      tm = null;
    }
    blackboard.unregisterInterest(watcher);
    if (alarmService != null) {
      serviceBroker.releaseService(this, AlarmService.class, alarmService);
      alarmService = null;
    }
    if (blackboard != null) {
      serviceBroker.releaseService(this, BlackboardService.class, blackboard);
      blackboard = null;
    }
    if (scheduler != null) {
      serviceBroker.releaseService(this, SchedulerService.class, scheduler);
      scheduler = null;
    }
  }

  //
  // implement basic "callback" actions
  //

  protected void precycle() {
    try {
      blackboard.openTransaction();
      setupSubscriptions();
    
      // run execute here so subscriptions don't miss out on the first
      // batch in their subscription addedLists
      execute();                // MIK: I don't like this!!!
    } catch (Throwable t) {
      System.err.println("Error: Uncaught exception in "+this+": "+t);
      t.printStackTrace();
    } finally {
      blackboard.closeTransaction();
    }
  }      
  
  protected void cycle() {
    // do stuff
    try {
      blackboard.openTransaction();
      if (wasAwakened() || blackboard.haveCollectionsChanged()) {
        execute();
      }
    } catch (Throwable t) {
      System.err.println("Error: Uncaught exception in "+this+": "+t);
      t.printStackTrace();
    } finally {
      blackboard.closeTransaction();
    }
  }
  
  /**
   * Called once after initialization, as a "pre-execute()".
   */
  protected abstract void setupSubscriptions();
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected abstract void execute();
  
  //
  // misc utility methods:
  //

  protected ConfigFinder getConfigFinder() {
    return ((PluginBindingSite) getBindingSite()).getConfigFinder();
  }
  
  /** 
   * @deprecated Use the self Organization or plugin parameters 
   * instead.  This method with be removed for cougaar 9.0.
   */
  protected ClusterIdentifier getClusterIdentifier() { 
    return ((PluginBindingSite) getBindingSite()).getAgentIdentifier();
  }
  
  /** storage for wasAwakened - only valid during cycle().
   **/
  private boolean awakened = false;

  /** true IFF were we awakened explicitly (i.e. we were asked to run
   * even if no subscription activity has happened).
   * The value is valid only within the scope of the cycle() method.
   */
  protected final boolean wasAwakened() { return awakened; }

  // for BlackboardClient use
  public synchronized String getBlackboardClientName() {
    if (blackboardClientName == null) {
      StringBuffer buf = new StringBuffer();
      buf.append(getClass().getName());
      if (parameters != null) {
        buf.append("[");
        String sep = "";
        for (Iterator params = parameters.iterator(); params.hasNext(); ) {
          buf.append(sep);
          buf.append(params.next().toString());
          sep = ",";
        }
        buf.append("]");
      }
      blackboardClientName = buf.substring(0);
    }
    return blackboardClientName;
  }
  
  public long currentTimeMillis() {
    if (alarmService != null)
      return alarmService.currentTimeMillis();
    else
      return System.currentTimeMillis();
  }
  
  // odd BlackboardClient method -- will likely be removed.
  public boolean triggerEvent(Object event) {
    return false;
  }
  
  public String toString() {
    return getBlackboardClientName();
  }
}
