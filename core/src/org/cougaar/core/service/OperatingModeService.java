package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.adaptivity.OperatingMode;
import org.cougaar.core.persist.NotPersistable;
import java.util.Set;

/** 
 * This service looks up operating modes in the blackboard.
 * For use by the AdaptivityEngine
 */
public interface OperatingModeService extends Service {
  /**
   * The interface to be implemented by listeners to this service.
   * Listeners are publishChanged when the state of operating modes
   * changes according to the options expressed by the listener.
   **/
  interface Listener extends NotPersistable {
    /**
     * Are additions wanted.
     * @return true if the listener is interested in additions of new
     * OperatingModes.
     **/
    boolean wantAdds();

    /**
     * Are changes wanted.
     * @return true if the listener is interested in changes to
     * existing OperatingModes.
     **/
    boolean wantChanges();

    /**
     * Are removes wanted.
     * @return true if the listener is interested in removal of
     * existing OperatingModes.
     **/
    boolean wantRemoves();
  }
  class ListenerAdapter {
    public boolean wantAdds() { return false; }
    public boolean wantChanges() { return false; }
    public boolean wantRemoves() { return false; }
  }

  /**
   * Add a listener to this service. Listeners are publishChanged
   * according to their interest when the status of operating modes
   * changes.
   * @param l the listener to add
   **/
  void addListener(Listener l);

  /**
   * Remove a listener to this service. Removed listeners will no
   * longer be publishChanged.
   * @param l the listener to remove
   **/
  void removeListener(Listener l);
  /**
   * Get an OperatingMode object by name.
   **/ 
  OperatingMode getOperatingModeByName(String knobName);

  /**
   * Get the names of all known SensorMeasurements.
   **/
  Set getAllOperatingModeNames();
}
