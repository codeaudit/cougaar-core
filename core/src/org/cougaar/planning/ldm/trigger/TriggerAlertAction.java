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

import org.cougaar.planning.ldm.plan.Alert;
import org.cougaar.core.plugin.PlugInDelegate;

/**
 * A trigger action to generate an alert
 */

public abstract class TriggerAlertAction implements TriggerAction {

  // Action Perform method : Generate given 
  public void Perform(Object[] objects, PlugInDelegate pid) {
    Alert new_alert = GenerateAlert(objects, pid);
    if (new_alert != null) {
      pid.publishAdd(new_alert);
    }
  }

  /**
   * Abstract method to generate alert for given objects
   * @param objects The objects to work from
   * @param pid  The PlugInDelegate to use for things like getClusterObjectFactory.
   * @return Alert  The new alert. 
   */
  public abstract Alert GenerateAlert(Object[] objects, PlugInDelegate pid);


}

