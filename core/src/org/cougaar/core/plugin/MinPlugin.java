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

import org.cougaar.core.cluster.SchedulerService;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedEvent;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.component.Trigger;



/**
 * Minimal plugin. It uses the SchedulerService for a shared thread, but otherwise does nothing
 *
 **/
public class MinPlugin 
  extends org.cougaar.util.GenericStateModelAdapter
  implements PluginBase 
{

  // Do we have a rule of thumb as to what should be private versus protected?
  protected boolean readyToRun = false;
  protected Trigger schedulerProd = null;
  protected boolean primed = false;
  protected SchedulerService myScheduler = null;
  private PluginBindingSite pluginBindingSite = null;


  public MinPlugin() { }

  /**
   * Found by introspection
   **/
  public void setBindingSite(BindingSite bs) {
    System.out.println("MinPlugin.setBindingSite() " + toString() );
    if (bs instanceof PluginBindingSite) {
      pluginBindingSite = (PluginBindingSite)bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }

    ServiceBroker myServiceBroker = pluginBindingSite.getServiceBroker();
    myScheduler = (SchedulerService )
      myServiceBroker.getService(this, SchedulerService.class, 
			    new ServiceRevokedListener() {
				public void serviceRevoked(ServiceRevokedEvent re) {
				  if (SchedulerService.class.equals(re.getService()))
				    myScheduler = null;
				}
			      });

    if (myScheduler != null) {
      Trigger pokeMe = new PluginCallback();
      // Tell him to schedule me, and get his callback object
      schedulerProd = myScheduler.register(pokeMe);
    }

  }

  protected PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }


  /**
   * Found by introspection by BinderSupport
   **/
  public void initialize() {
    // Tell the scheduler to run me at least this once
    schedulerProd.trigger();
  }

  /**
   * This is the scheduler's hook into me
   **/
  protected class PluginCallback implements Trigger {
    public void trigger() {
      System.out.println("PluginCallback.trigger()");
      if (!primed) {
	precycle();
      }
      if (readyToRun) {
	cycle();
      }
    }
  }

  protected void precycle() {
    primed = true;
  }

  protected void cycle() {
    // do stuff
    readyToRun = false;
  }

}
