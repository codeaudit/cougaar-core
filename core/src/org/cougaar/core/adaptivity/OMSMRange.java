package org.cougaar.core.adaptivity;

/**
 * Holds a range specification for an operating mode value or sensor
 * measurement. Ranges are half-open intervals. The value of max
 * _must_ exceed low.
 **/
public class OMSMRange {
  protected Comparable min, max;

  protected OMSMRange(int min, int max) {
    this(new Integer(min), new Integer(max));
  }

  protected OMSMRange(double min, double max) {
    this(new Double(min), new Double(max));
  }

  protected OMSMRange(Comparable min, Comparable max) {
    if (min.getClass() != max.getClass()) {
      throw new IllegalArgumentException("Min and max have different classes");
    }
    if (min.compareTo(max) > 0) {
      throw new IllegalArgumentException("Min must not exceed max");
    }
    this.min = min;
    this.max = max;
  }

  public boolean contains(Comparable v) {
    return min.compareTo(v) <= 0 && max.compareTo(v) >= 0;
  }

  public Comparable getMin() {
    return min;
  }

  public Comparable getMax() {
    return max;
  }
}
