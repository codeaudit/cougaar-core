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
 * Schedules plugins. Tells them to run by poking them
 */
public interface SchedulerService {

  /**
   * Tells Scheduler to handle scheduling this object
   * @param managedItem the trigger to be pulled to run the object
   * @return a handle that the caller can use to tell the scheduler that it wants to be run.
   **/
  Trigger register(Trigger managedItem);

  /**
   * @param removeMe Stop scheduling this item
   **/
  void unregister(Trigger removeMe);
}

