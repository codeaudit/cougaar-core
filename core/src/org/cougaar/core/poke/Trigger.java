/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.poke;

import org.cougaar.core.component.*;

/**
 * callback interface. 
 **/
public interface Trigger {

  /**
   * callback to activate object
   *
   * Two similar implimentations are currently planned for this interface, 
   * one inside of Plugin, the other in Scheduler. 
   * The Plugin would use the Scheduler's Trigger to tell the Scheduler it wants to run.
   * The Scheduler would use the Plugin's Trigger eventually to run the Plugin.
   **/
  void trigger();
}
