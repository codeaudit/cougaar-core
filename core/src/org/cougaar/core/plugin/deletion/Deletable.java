/* 
 * <copyright>
 * Copyright 2004 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.plugin.deletion;

/**
 * @author RTomlinson
 *
 * Define the interface for blackboard objects that can be deleted. Deletion
 * occurs when a blackboard object is no longer functionally significant
 * subject to a DeletionPolicy that may extend the interval before an object
 * is actually removed to support "best effort" retention of historical data.
 * Actual deletion is performed by deletion plugins according to policies. Not
 * all deletable blackboard objects implement this interface. It is only
 * implemented for simple deletable objects for which the "is it time to
 * delete" decision is uncomplicated.
 */
public interface Deletable {
  /**
   * Some Deletable objects are not actually deletable. This method lets such
   * objects declare whether they are deletable or not.
   * @return true if the object with this interface can be deleted.
   */
  boolean isDeletable();
  
  /**
   * Indicates that this object has been deleted and that remove events should
   * be interpreted as deletion rather than rescind.
   * @return true if this object has been deleted.
   */
  boolean isDeleted();
  
  /**
   * Set the status of this object to "deleted" (isDeleted should return true).
   * There is no argument because there is no provision for clearing the
   * deleted status of an object.
   */
  void setDeleted();
  
  /**
   * Get the deletion time of this object. The deletion time, in conjunction
   * with the applicable DeletionPolicy determines the earliest time when an
   * object can be safely deleted. This time should be as early as possible
   * consistent with correct operation. A DeletionPolicy should be used to
   * extend the time an object remains on the blackboard for historical reasons.
   * @return The time (scenario time) in milliseconds at which this object can
   * be safely deleted.
   */
  long getDeletionTime();
  
  /**
   * Specifies that the time returned by getDeletion() is based on the value of
   * System.currentTimeMillis() rather than scenario time.
   * @return whether it uses scenario or system time
   */
  boolean useSystemTime();
}
