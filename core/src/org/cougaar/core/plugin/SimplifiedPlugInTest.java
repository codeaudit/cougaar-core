/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.IncrementalSubscription;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;


public class SimplifiedPlugInTest extends SimplifiedPlugIn {
  private Subscription allMyAssets;
  
  private static UnaryPredicate assetPredicate() {
    return new UnaryPredicate() {
      public boolean execute(Object o) {
	return (o instanceof Asset);
      }
    };
  }

  protected void setupSubscriptions() {
    allMyAssets = subscribe(assetPredicate());

    // wake in 5 seconds if nothing else happens
    wakeAfter(5000);
  }

  protected void execute() {
    System.err.println("\nSimplifiedPlugInTest.execute() running: I see "+
		       ((IncrementalSubscription)allMyAssets).getCollection().size() + " Assets.");
    // reset the timer
    wakeAfter(5000);
  }
}
