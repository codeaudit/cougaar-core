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
import org.cougaar.core.plugin.PluginDelegate;

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
  public void Perform(Object[] objects, PluginDelegate pid) {
    // Make my_allocation stale  (don't really need the passed in object array)
    my_allocation.setStale(true);
    pid.publishChange(my_allocation);
    //System.err.println("Made it stale");
  }

  
 

}

