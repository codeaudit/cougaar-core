package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.adaptivity.SensorMeasurement;
import java.util.Set;

/** 
 * This service looks up sensor data in the blackboard, 
 * For use by the AdaptivityEngine
 */
public interface SensorMeasurementService extends Service {
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
   * Get a SensorMeasurement object by name.
   **/ 
  SensorMeasurement getSensorMeasurementByName(String sensor);

  /**
   * Get the names of all known SensorMeasurements.
   **/
  Set getAllSensorMeasurementNames();

  /**
   * Add a listener object. The given object will be publishChanged
   * whenever any SensorMeasurement is added, removed, or changed. The
   * Object must already have been publishedAdded to the blackboard by
   * the caller.
   **/
  void addListener(Listener cb);
  /**
   * Remove a listener object. The given object will no longer be
   * publishChanged whenever any SensorMeasurement is added, removed,
   * or changed. The Object should not be removed from the blackboard
   * until it has been removed as a listener.
   **/
  void removeListener(Listener cb);
}


