package org.cougaar.core.adaptivity;

public interface SensorMeasurementListener {
    void sensorMeasurementChanged(SensorMeasurement m, Comparable oldValue);
}
