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

