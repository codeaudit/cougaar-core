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

import org.cougaar.util.ConfigFinder;
import org.cougaar.core.component.Trigger;
import org.cougaar.util.TriggerModel;
import org.cougaar.util.SyncTriggerModelImpl;

import org.cougaar.core.cluster.SchedulerService;

/**
 * A minimal Plugin that illustrates use of the scheduler 
 * service.
 */
public abstract class MinPlugin
  extends org.cougaar.util.GenericStateModelAdapter
  implements PluginBase
{
  
  protected Object parameter;

  protected BindingSite bindingSite;
  protected ServiceBroker serviceBroker;

  protected SchedulerService scheduler;
  protected TriggerModel tm;
  
  public MinPlugin() { 
  }
  
  /**
   * Called just after construction (via introspection) by the 
   * loader if a non-null parameter Object was specified by
   * the ComponentDescription.
   **/
  public void setParameter(Object param) {
    this.parameter = param;
  }
  
  /**
   * Binding site is set by reflection at creation-time.
   */
  public void setBindingSite(BindingSite bs) {
    this.bindingSite = bs;
    this.serviceBroker = bs.getServiceBroker();
  }

  //
  // implement GenericStateModel:
  //

  public void initialize() {
    super.initialize();
  }

  public void load() {
    super.load();
    
    // obtain the scheduler service
    scheduler = (SchedulerService)
      serviceBroker.getService(
          this,
          SchedulerService.class,
          null);
    if (scheduler == null) {
      throw new RuntimeException(
          this+" unable to obtain scheduler");
    }

    // create a callback for running this component
    Trigger myTrigger = 
      new Trigger() {
        public void trigger() {
          cycle();
        }
        public String toString() {
          return MinPlugin.this.toString();
        }
      };

    // create the trigger model
    this.tm = new SyncTriggerModelImpl(scheduler, myTrigger);

    // activate the trigger model
    tm.initialize();
    tm.load();
  }

  public void start() {
    super.start();
    tm.start();
    // run "cycle()" at least once
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
    if (scheduler != null) {
      serviceBroker.releaseService(
          this, SchedulerService.class, scheduler);
      scheduler = null;
    }
  }

  /**
   * "cycle()" is called by the scheduler thread to perform
   * MinPlugin activity.
   * <p>
   * The first "cycle()" is requested by "start()".  Later cycles 
   * can be requested by using "tm.trigger()", from either within
   * "cycle()" itself or from another class (e.g. a blackboard 
   * watcher).
   * <p>
   * Typically the first "cycle()" is used as an initialization
   * step, so a "boolean didPrecycle" is kept in the subclass.
   * <p>
   * This method should not be synchronized, since it is only
   * called by the scheduler's (synchronized) trigger model.
   */
  protected abstract void cycle();

}
