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
import org.cougaar.core.component.Trigger;

import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.core.cluster.AlarmService;
import org.cougaar.core.cluster.SchedulerService;
import org.cougaar.core.cluster.SubscriptionWatcher;

import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.util.ConfigFinder;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

/**
 * first new-fangled plugin. It doesn't do much, but it
 * holds on to its own blackboard subscription watcher.
 * Uses new SchedulerService.
 *
 * Use it as a base class. Make a derived class simply by overriding 
 * setupSubscriptions() and execute()
 **/
public class ComponentPlugin 
  extends org.cougaar.util.GenericStateModelAdapter
  implements PluginBase, BlackboardClient 
{
  
  // Do we have a rule of thumb as to what should be private versus protected?
  protected boolean readyToRun = false;
  protected SchedulerService myScheduler = null;
  protected Trigger schedulerProd = null;
  protected BlackboardService blackboard = null;
  protected AlarmService alarmService = null;
  protected boolean primed = false;
  private PluginBindingSite pluginBindingSite = null;
  private ServiceBroker serviceBroker = null;
  private ThinWatcher watcher = null;
  private Collection parameters = null;
  
  public ComponentPlugin() { }   
  
  /**
   * BlackboardClient implementation 
   * BlackboardService access requires the requestor to implement BlackboardClient
   **/
  protected String blackboardClientName = null;
  
  public String getBlackboardClientName() {
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
  
  public boolean triggerEvent(Object event) {
    return false;
  }
  
  /**
   * Service found by introspection
   **/
  public void setBindingSite(BindingSite bs) {
    if (bs instanceof PluginBindingSite) {
      pluginBindingSite = (PluginBindingSite)bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
    serviceBroker = pluginBindingSite.getServiceBroker();
  }

  public final void setSchedulerService(SchedulerService ss) {
    myScheduler = ss;
  }
  public final void setBlackboardService(BlackboardService bs) {
    blackboard = bs;
  }
  public final void setAlarmService(AlarmService s) {
    alarmService = s;
  }
  public void load() {
    super.load();
    /*
      // rely on setSchedulerService for now - don't worry about revokation
    myScheduler = (SchedulerService )
      serviceBroker.getService(
          this, 
          SchedulerService.class, 
          new ServiceRevokedListener() {
            public void serviceRevoked(ServiceRevokedEvent re) {
              if (SchedulerService.class.equals(re.getService()))
                myScheduler = null;
              }
            });
    */
    

    if (myScheduler != null) {
      Trigger pokeMe = new PluginCallback();
      // Tell him to schedule me, and get his callback object
      schedulerProd = myScheduler.register(pokeMe);
    }
    

    /*
    // proceed to get blackboard service
    blackboard = (BlackboardService)
      serviceBroker.getService(
          this, 
          BlackboardService.class,
          new ServiceRevokedListener() {
            public void serviceRevoked(ServiceRevokedEvent re) {
              if (BlackboardService.class.equals(re.getService())) {
                blackboard = null;
                watcher = null;
              }
            }
          });
    */
    
    /*
    // proceed to get alarm service
    alarmService = (AlarmService)
      serviceBroker.getService(
          this, 
          AlarmService.class,
          new ServiceRevokedListener() {
            public void serviceRevoked(ServiceRevokedEvent re) {
              if (AlarmService.class.equals(re.getService())) {
                alarmService = null;
              }
            }
          });
    */
    
    // someone to watch over me
    watcher = new ThinWatcher();
    if (blackboard != null) {
      blackboard.registerInterest(watcher);
    } else {
      System.out.println("ComponentPlugin:setBindingSite() !!No Blackboard - oh my");
    }
    
  }

  public void unload() {
    super.unload();
    if (alarmService != null) {
      serviceBroker.releaseService(this, AlarmService.class, alarmService);
    }
    if (blackboard != null) {
      serviceBroker.releaseService(this, BlackboardService.class, blackboard);
    }
    if (myScheduler != null) {
      serviceBroker.releaseService(this, SchedulerService.class, myScheduler);
    }
  }

  /**
   * accessor for my bindingsite - interface to by binder
   **/
  protected PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }
  
  /** 
   * accessor for my servicebroker - use this to request services 
   **/
  protected ServiceBroker getServiceBroker() {
    return serviceBroker;
  }
  
  /**
   * accessor for the blackboard service
   **/
  protected BlackboardService getBlackboardService() {
    return blackboard;
  }
  
  /**
   * Found by introspection by BinderSupport
   **/
  
  public void start() {
    super.start();
    // Tell the scheduler to run me at least this once
    schedulerProd.trigger();
  }
  
  /**
   * Found by introspection by ComponentFactory
   * PM expects this, and fails if it isn't here.
   **/
  public void setParameter(Object param) {
    if (param != null) {
      parameters = (Vector) param;
    } else {
      parameters = new Vector(0);
    }
  }
  
  /** get any Plugin parameters passed by the plugin instantiator.
   * If they haven't been set, will return null.
   * Should be set between plugin construction and initialization.
   **/
  public Collection getParameters() {        
    return parameters;
  }
  
  /*
    public Vector getParameters() {
    return parameters;
    }
  */
  
  /** let subclasses get ahold of the cluster without having to catch it at
   * load time.  May throw a runtime exception if the plugin hasn't been 
   * loaded yet.
   * 
   **/
  
  protected ConfigFinder getConfigFinder() {
    return ((PluginBindingSite) getBindingSite()).getConfigFinder();
  }
  
  /** @deprecated Use the self Organization or plugin parameters instead.  This method with
   * be removed for cougaar 9.0.
   **/
  protected ClusterIdentifier getClusterIdentifier() { 
    return ((PluginBindingSite) getBindingSite()).getAgentIdentifier();
  }
  
  
  /**
   * This is the scheduler's hook into me
   **/
  protected class PluginCallback implements Trigger {
    public synchronized void trigger() {
      if (!primed) {
        precycle();
      }
      if (readyToRun) { 
        cycle();
      }
    }
  }
  
  protected void precycle() {
    try {
      blackboard.openTransaction();
      setupSubscriptions();
    
      // run execute here so subscriptions don't miss out on the first
      // batch in their subscription addedLists
      execute();                // MIK: I don't like this!!!
    
      readyToRun = false;  // don't need to run execute again
    } catch (Throwable t) {
      System.err.println("Error: Uncaught exception in "+this+": "+t);
      t.printStackTrace();
    } finally {
      blackboard.closeTransaction();
      primed = true;
    }
  }      
  
  protected void cycle() {
    // do stuff
    readyToRun = false;
    try {
      blackboard.openTransaction();
      execute();
    } catch (Throwable t) {
      System.err.println("Error: Uncaught exception in "+this+": "+t);
      t.printStackTrace();
    } finally {
      blackboard.closeTransaction();
    }
  }
  
  /**
   * override me
   * Called once sometime after initialization
   **/
  protected void setupSubscriptions() {}
  
  /**
   * override me
   * Called everytime plugin is scheduled to run
   **/
  protected void execute() {}
  
  public String toString() {
    return getBlackboardClientName();
  }
  
  protected class ThinWatcher extends SubscriptionWatcher {
    /** Override this method so we don't have to do a wait()
     */
    public void signalNotify(int event) {
      // gets called frequently as the blackboard objects change
      super.signalNotify(event);
      
      // ask the scheduler to run us again.
      if (schedulerProd != null) {
        readyToRun = true;
        schedulerProd.trigger();
      }
    }
  }
}
