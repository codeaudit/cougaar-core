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

import org.cougaar.core.component.*;
import org.cougaar.core.plugin.PluginBase;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.blackboard.BlackboardService;
import org.cougaar.core.plugin.PluginBindingSite;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Iterator;

/**
 * first new-fangled plugin. It doesn't do much, but it
 * holds on to its own blackboard subscription watcher
 **/
public class PokePlugin implements PluginBase, BlackboardClient {

  protected boolean readyToRun = false;
  protected SchedulerService myScheduler = null;
  protected Pokable schedulerProd = null;
  protected BlackboardService blackboard = null;
  protected boolean primed = false;
  private PluginBindingSite pluginBindingSite = null;
  private SubscriptionWatcher watcher = null;
  private final String myName = "SlimShady";

  public PokePlugin() { }

  /**
   *  Totally bogus BlackboardClient implementation
   **/
  public String getBlackboardClientName() {
    return myName;
  }
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  public boolean triggerEvent(Object event) {
    return false;
  }

  /**
   * Found by introspection
   **/
  public void setBindingSite(BindingSite bs) {
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
				  if (SchedulerService.class.equals(re.getRevokedService()))
				    myScheduler = null;
				}
			      });

    if (myScheduler != null) {
      Pokable pokeMe = new PluginCallback();
      // Tell him how to reach me
      myScheduler.setPokable(this, pokeMe);
      // Tell him to schedule me, and get his callback object
      schedulerProd = myScheduler.register(pokeMe);
    }

    blackboard = (BlackboardService)
      myServiceBroker.getService(this, BlackboardService.class,
 			    new ServiceRevokedListener() {
				public void serviceRevoked(ServiceRevokedEvent re) {
				  if (BlackboardService.class.equals(re.getRevokedService())) {
				    blackboard = null;
				    watcher = null;
				  }
				}
			      });

    // someone to watch over me
    watcher = new ThinWatcher();
    if (blackboard != null) {
      blackboard.registerInterest(watcher);
    } else {
      System.out.println("PokePlugin:setBindingSite() !!No Blackboard - oh my");
    }

  }


  /**
   * Found by introspection by BinderSupport
   **/
  public void initialize() {
    // poke him to see what he'll do
    System.out.println("PokePlugin.initialize() - poking scheduler");
    schedulerProd.poke();
  }

  /**
   * Found by introspection by PluginManager
   * PM expects this, and fails if it isn't here.
   **/
  public void setParameter(Object param) {
  }

  /**
   * This is the scheduler's hook into me
   **/
  protected class PluginCallback implements Pokable {
    public void poke() {
      System.out.println("PluginCallback.poke()");
      if (!primed) {
	precycle();
      }
      if (readyToRun) {
	cycle();
      }
    }
  }

  private final UnaryPredicate stringPred = 
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof String) 
	    return true;
	  return false;
	}
      };
  
  private IncrementalSubscription stringSubscription = null;

  protected void precycle() {
    // set up stuff 
    System.out.println("PokePlugin.precycle()");
    primed = true;

    if (blackboard != null) {
      blackboard.openTransaction();
      blackboard.publishAdd("A scribble on the blackboard");
      stringSubscription = (IncrementalSubscription) blackboard.subscribe(stringPred);
      blackboard.closeTransaction();
    }
  }

  protected void cycle() {
    System.out.println("PokePlugin.cycle()");
    readyToRun = false;

    // do stuff
    blackboard.openTransaction();
    if (stringSubscription.hasChanged()) {
      for (Iterator it = stringSubscription.iterator(); it.hasNext();) {
	System.out.println("PokePlugin.cycle() - found on blackoard: " + it.next());
      }
    }
    blackboard.closeTransaction();
  }

  protected class ThinWatcher extends SubscriptionWatcher {
    /** Override this method so we don't have to do a wait()
     */
    public void signalNotify(int event) {
      super.signalNotify(event);
      // ask the scheduler to run us again.
      if (schedulerProd != null) {
	readyToRun = true;
	schedulerProd.poke();
      }
    }
  }
}
