package org.cougaar.core.adaptivity;

import java.util.*;
import org.cougaar.planning.ldm.policy.RuleParameter;

/**
 * The base class for OperatingModes and SensorMeasurements. These two
 * concepts differ only in that an OperatingMode can be set by the
 * AdaptivityEngine whereas a SensorMeasurement is under the control
 * of the Sensor making the measurement.
 **/
public abstract class OMSMBase implements java.io.Serializable {
  private String name;
  private Comparable value;
  private OMSMValueList allowedValues;

  protected OMSMBase(String name, OMSMValueList allowedValues) {
    this.name = name;
    this.allowedValues = allowedValues;
    this.value = allowedValues.getEffectiveValue();
  }
  protected OMSMBase(String name, OMSMValueList allowedValues, Comparable initialValue) {
    this(name, allowedValues);
    setValue(initialValue);
  }

  public String getName() {
    return name;
  }
    
  public OMSMValueList getAllowedValues() {
    return allowedValues;
  }

  public Comparable getValue() {
    return value;
  }

  protected abstract void fireListeners(Comparable oldValue);

  protected void setValue(Comparable newValue) {
    if (!allowedValues.isAllowed(newValue)) {
      throw new IllegalArgumentException("setValue: value not allowed");
    }
    if (value.compareTo(newValue) == 0) return; // Already set to this value
    Comparable oldValue = getValue();
    value = newValue;
    fireListeners(oldValue);
  }
}
