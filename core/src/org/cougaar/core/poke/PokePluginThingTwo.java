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

package org.cougaar.core.poke;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.util.UnaryPredicate;

import java.util.Collection;
import java.util.Iterator;

/**
 * A really dumb plugin that writes and reads a string to/from the blackboard
 **/
public class PokePluginThingTwo extends ComponentPlugin {

  private String me = null;

  public PokePluginThingTwo() { }


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

    System.out.println("PokePluginThingTwo.setupSubscriptions() " + me);
    blackboard.publishAdd("A scribble on the blackboard by " + me);
    stringSubscription = (IncrementalSubscription) blackboard.subscribe(stringPred);
  }

  protected void execute() {
    System.out.println("PokePluginThingTwo.execute() " + me);

    if (stringSubscription.hasChanged()) {
      for (Iterator it = stringSubscription.iterator(); it.hasNext();) {
	System.out.println("PokePluginThingTwo.cycle() " + me + "  - found on blackoard: " + it.next());
      }
    }
  }

  protected void parseParams() {
    Iterator it = getParameters().iterator();
    me = it.next().toString();
  }

}
