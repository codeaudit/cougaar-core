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
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * A really dumb plugin that writes and reads a string to/from the blackboard
 **/
public class PokePluginThingOne extends PokePlugin {

  private String me = null;

  public PokePluginThingOne() { }


  private final UnaryPredicate stringPred = 
    new UnaryPredicate() {
	public boolean execute(Object o) {
	  if (o instanceof String) 
	    return true;
	  return false;
	}
      };
  
  private IncrementalSubscription stringSubscription = null;

  protected void setupSubscriptions() {
    // set up stuff 
    parseParams();

    System.out.println("PokePluginThingOne.setupSubscriptions() " + me);
    blackboard.publishAdd("A scribble on the blackboard by " + me);
    stringSubscription = (IncrementalSubscription) blackboard.subscribe(stringPred);
  }

  protected void execute() {
    System.out.println("PokePluginThingOne.execute() " + me);
    readyToRun = false;

    if (stringSubscription.hasChanged()) {
      for (Iterator it = stringSubscription.iterator(); it.hasNext();) {
	System.out.println("PokePluginThingOne.cycle() " + me + "  - found on blackoard: " + it.next());
      }
    }
  }

  protected void parseParams() {
    Iterator it = getParameters().iterator();
    me = it.next().toString();
  }

}
