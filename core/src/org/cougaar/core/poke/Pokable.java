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
public interface Pokable {

  /**
   * callback to activate object
   *
   * Two similar implimentations are currently planned for this interface, 
   * one inside of Plugin, the other in the Poker (Scheduler). The first 
   * implementation is for a Service (Blackboard or Alarm, for example)
   * to call when it has  completed a job for the Plugin. The 
   * implementation would call 
   * the other implementation - the Poker's Pokable. The Pokable 
   * implementation in the Poker would schedule the Plugin (or actually 
   * the Plugin's  Pokable implementation) to be run, and eventually the 
   * Poker would call Pokable.cycle(), which would allow the Plugin to do it's job.
   **/
  void poke();
}
