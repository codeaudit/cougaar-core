package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.adaptivity.OperatingMode;
import java.util.Set;

/** 
 * This service looks up operating modes in the blackboard.
 * For use by the AdaptivityEngine
 */
public interface OperatingModeService extends Service {
  /**
   * Get an OperatingMode object by name.
   **/ 
  OperatingMode getOperatingModeByName(String knobName);

  /**
   * Get the names of all known SensorMeasurements.
   **/
  Set getAllOperatingModeNames();
}
