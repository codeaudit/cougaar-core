package org.cougaar.core.adaptivity;

/**
 * Holds a range specification for an operating mode value or sensor
 * measurement. Ranges are half-open intervals. The value of max
 * _must_ exceed low.
 **/
public class OMSMPoint extends OMSMRange {
  public OMSMPoint(int v) {
    this(new Integer(v));
  }

  public OMSMPoint(double v) {
    this(new Double(v));
  }

  public OMSMPoint(Comparable v) {
    super(v, v);
  }

  public String toString() {
    return min.toString();
  }
}
