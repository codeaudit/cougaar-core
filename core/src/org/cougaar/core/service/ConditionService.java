/*
 * <copyright>
 *  Copyright 2003 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import java.util.Set;

import org.cougaar.core.adaptivity.Condition;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;

/** 
 * This service looks up sensor data in the blackboard, 
 * For use by the AdaptivityEngine
 */
public interface ConditionService extends Service {
  /**
   * The interface to be implemented by listener objects for this
   * service. Note that no implementation of the methods of this
   * interface should do any more than set variables within the
   * object. In particular, they should not use any services or
   * synchronize on or wait for anything.
   *
   * There are currently no methods defined
   **/
  interface Listener extends NotPersistable {
  }

  /**
   * Get a Condition object by name.
   **/ 
  Condition getConditionByName(String sensor);

  /**
   * Get the names of all known Conditions.
   **/
  Set getAllConditionNames();

  /**
   * Add a listener object. The given object will be publishChanged
   * whenever any Condition is added, removed, or changed. The
   * Object must already have been publishedAdded to the blackboard by
   * the caller.
   **/
  void addListener(Listener cb);
  /**
   * Remove a listener object. The given object will no longer be
   * publishChanged whenever any Condition is added, removed,
   * or changed. The Object should not be removed from the blackboard
   * until it has been removed as a listener.
   **/
  void removeListener(Listener cb);
}


