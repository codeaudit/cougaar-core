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
public class ComponentPlugin implements PluginBase, BlackboardClient {

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
   *  somewhat bogus BlackboardClient implementation
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
   * Found by introspection
   **/
  public void setBindingSite(BindingSite bs) {
    //System.out.println("ComponentPlugin.setBindingSite() " + toString() );
    if (bs instanceof PluginBindingSite) {
      pluginBindingSite = (PluginBindingSite)bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }

    serviceBroker = pluginBindingSite.getServiceBroker();
    myScheduler = (SchedulerService )
      serviceBroker.getService(this, SchedulerService.class, 
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

    blackboard = (BlackboardService)
      serviceBroker.getService(this, BlackboardService.class,
 			    new ServiceRevokedListener() {
				public void serviceRevoked(ServiceRevokedEvent re) {
				  if (BlackboardService.class.equals(re.getService())) {
				    blackboard = null;
				    watcher = null;
				  }
				}
			      });

    alarmService = (AlarmService)
      serviceBroker.getService(this, AlarmService.class,
 			    new ServiceRevokedListener() {
				public void serviceRevoked(ServiceRevokedEvent re) {
				  if (AlarmService.class.equals(re.getService())) {
				    alarmService = null;
				  }
				}
			      });


    // someone to watch over me
    watcher = new ThinWatcher();
    if (blackboard != null) {
      blackboard.registerInterest(watcher);
    } else {
      System.out.println("ComponentPlugin:setBindingSite() !!No Blackboard - oh my");
    }

  }

  protected PluginBindingSite getBindingSite() {
    return pluginBindingSite;
  }


  protected ServiceBroker getServiceBroker() {
    return serviceBroker;
  }


  /**
   * Found by introspection by BinderSupport
   **/
  public void initialize() {
    // Tell the scheduler to run me at least this once
    schedulerProd.trigger();
  }

  /**
   * Found by introspection by ComponentFactory
   * PM expects this, and fails if it isn't here.
   **/
  public void setParameter(Object param) {
    //System.out.println("ComponentPlugin.setParameter()");
    if (param != null) {
      if (param instanceof Collection) {
	parameters = (Collection) param;
      } else {
	System.err.println("Warning: "+this+" initialized with non-collection parameter "+param);
      }
    }
  }

  /** get any Plugin parameters passed by the plugin instantiator.
   * If they haven't been set, will return null.
   * Should be set between plugin construction and initialization.
   **/
  public Collection getParameters() {
    return parameters;
  }


  /**
   * This is the scheduler's hook into me
   **/
  protected class PluginCallback implements Trigger {
    public void trigger() {
      //System.out.println("PluginCallback.trigger()");
      if (!primed) {
	precycle();
      }
      if (readyToRun) {
	cycle();
      }
    }
  }

  protected void precycle() {
    blackboard.openTransaction();
    setupSubscriptions();
    blackboard.closeTransaction();
    primed = true;
  }

  protected void cycle() {
    // do stuff
    readyToRun = false;
    blackboard.openTransaction();
    execute();
    blackboard.closeTransaction();
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
      // This seems to get called even though my subscriptions haven't changed. Why?
      super.signalNotify(event);
      //System.out.println("ThinWatcher.signalNotify(" + event + ")");
      // ask the scheduler to run us again.
      if (schedulerProd != null) {
	readyToRun = true;
	schedulerProd.trigger();
      }
    }
  }
}
