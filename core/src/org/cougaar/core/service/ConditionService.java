
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


