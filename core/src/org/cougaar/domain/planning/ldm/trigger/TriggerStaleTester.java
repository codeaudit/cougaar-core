/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */



package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.domain.planning.ldm.plan.Allocation;

import java.util.List;
import java.util.Arrays;
import java.util.ListIterator;

/**
 * A Trigger Tester to determine if an allocation is stale
 */

public class TriggerStaleTester implements TriggerTester {
  private transient boolean stale;

  /** 
   * Return indication if any allocation in group is stale
   */
  public boolean Test(Object[] objects) {
    // Check if any of the objects are 'stale' allocations
    // reset stale flag each time
    stale = false;
    List objectlist = Arrays.asList(objects);
    ListIterator lit = objectlist.listIterator();
    while ( lit.hasNext() ) {
      // just to be safe for now, get the object as an Object and 
      // check if its an Allocation before checking the stale flag.
      Object o = (Object)lit.next();
      if (o instanceof Allocation) {
        if ( ((Allocation)o).isStale() ) {
          stale = true;
        }
      }
    }
    //System.err.println("TriggerStaleTester returning: "+stale);
    return stale;
  }


}
