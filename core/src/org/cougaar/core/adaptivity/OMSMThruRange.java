package org.cougaar.core.adaptivity;

/**
 * Holds a range specification for an operating mode value or sensor
 * measurement. Ranges are half-open intervals. The value of max
 * _must_ exceed low.
 **/
public class OMSMThruRange extends OMSMRange {
  public OMSMThruRange(int min, int max) {
    super(new Integer(min), new Integer(max));
  }

  public OMSMThruRange(double min, double max) {
    super(new Double(min), new Double(max));
  }

  public OMSMThruRange(Comparable min, Comparable max) {
    super(min, max);
  }

  public String toString() {
    return min.toString() + " thru " + max.toString();
  }
}
