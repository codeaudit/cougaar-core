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
import org.cougaar.core.plugin.PlugInDelegate;

/**
 * TriggerAction to make given allocation object
 */

public class TriggerMakeStaleAction implements TriggerAction {
  
  // Private variables
  private Allocation my_allocation;

  public TriggerMakeStaleAction(Allocation allocation) { 
    my_allocation = allocation; 
  }

  // Make given allocation object stale when fired
  public void Perform(Object[] objects, PlugInDelegate pid) {
    // Make my_allocation stale  (don't really need the passed in object array)
    my_allocation.setStale(true);
    pid.publishChange(my_allocation);
    //System.err.println("Made it stale");
  }

  
 

}

