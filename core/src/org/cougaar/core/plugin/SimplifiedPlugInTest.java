/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.IncrementalSubscription;

import org.cougaar.domain.planning.ldm.asset.Asset;
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
