package org.cougaar.core.adaptivity;
import java.util.*;

/** 
 * A class to hold a sensor measurement. Most sensors will subclass
 * this class to add a private or package protected setValue method to
 * preclude arbitrary classes from messing with the value.
 */
public class SensorMeasurement extends OMSMBase {
  private transient List listeners = new ArrayList(1);

  public SensorMeasurement(String name, OMSMValueList allowedValues) {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }
  public SensorMeasurement(String name, OMSMValueList allowedValues, Comparable value) {
    super(name, allowedValues, value);
  }
    
  public void addSensorMeasurementListener(SensorMeasurementListener l) {
    listeners.add(l);
  }

  public void removeSensorMeasurementListener(SensorMeasurementListener l) {
    listeners.remove(l);
  }

  protected void fireListeners(Comparable oldValue) {
    if (listeners == null) return; // No listeners yet
    for (Iterator j = listeners.iterator(); j.hasNext(); ) {
      SensorMeasurementListener l = (SensorMeasurementListener) j.next();
      try {
        l.sensorMeasurementChanged(this, oldValue);
      } catch (Exception e) {
//          logger.error("Exception firing listeners", e);
      }
    }
  }

  public String toString() {
    return getName() + " = " + getValue();
  }
}
