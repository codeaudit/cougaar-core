/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.cluster;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.Trigger;

/**
 * Schedules plugins. Tells them to run by poking them
 */
public interface SchedulerService extends Service {

  /**
   * Tells Scheduler to handle scheduling this object.  <p>
   * <em>IMPORTANT</em> Note that it is possible for the
   * trigger to be invoked in parallel under some scheduler service 
   * implementations, so it is probably a good idea for scheduled
   * Trigger.trigger() methods to be synchronized.
   * @param managedItem the trigger to be pulled to run the object
   * @return a handle that the caller can use to tell the scheduler that it wants to be run.
   **/
  Trigger register(Trigger managedItem);

  /**
   * @param removeMe Stop scheduling this item
   **/
  void unregister(Trigger removeMe);
}

