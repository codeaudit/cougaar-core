package org.cougaar.core.adaptivity;

/**
 * Holds a range specification for an operating mode value or sensor
 * measurement. Ranges are half-open intervals. The value of max
 * _must_ exceed low.
 **/
public class OMSMToRange extends OMSMRange {
  private Comparable undecrementedMax;

  public OMSMToRange(int min, int max) {
    this(new Integer(min), new Integer(max));
  }

  public OMSMToRange(double min, double max) {
    this(new Double(min), new Double(max));
  }

  public OMSMToRange(Comparable min, Comparable max) {
    super(min, ComparableHelper.decrement(max));
    undecrementedMax = max;
  }

  public String toString() {
    return min.toString() + " to " + undecrementedMax.toString();
  }
}
