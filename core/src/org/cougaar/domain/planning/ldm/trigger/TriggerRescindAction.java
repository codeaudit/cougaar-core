/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.core.plugin.PlugInDelegate;

/**
 * Trigger action to rescind given Plan Element when fired
 */

public class TriggerRescindAction implements TriggerAction {
  
  private PlanElement my_pe;

  public TriggerRescindAction(PlanElement pe) { my_pe = pe; }

  public void Perform(Object[] objects, PlugInDelegate pid) {
    // publishRemove designated plan element  (we don't really need the objects)
    // make sure the PlanElement is not null
    if (my_pe != null) {
      //System.out.println("TriggerRescindAction rescinding my_pe!");
      pid.publishRemove(my_pe);
    }
  }


}

