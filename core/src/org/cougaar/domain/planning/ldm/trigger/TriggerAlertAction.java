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

import org.cougaar.domain.planning.ldm.plan.Alert;
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

