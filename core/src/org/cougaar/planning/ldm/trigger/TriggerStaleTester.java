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



package org.cougaar.planning.ldm.trigger;

import org.cougaar.planning.ldm.plan.Allocation;

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
