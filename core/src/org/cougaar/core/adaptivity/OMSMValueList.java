package org.cougaar.core.adaptivity;

import java.util.List;
import java.util.ArrayList;

public class OMSMValueList {
  OMSMRange[] allowedValues;

  public OMSMValueList(OMSMRange[] av) {
    allowedValues = av;
  }

  public OMSMValueList(OMSMRange av) {
    allowedValues = new OMSMRange[] {av};
  }

  public OMSMValueList(double v) {
    this(new OMSMRange[] {new OMSMPoint(v)});
  }

  public OMSMValueList(int v) {
    this(new OMSMRange[] {new OMSMPoint(v)});
  }

  public OMSMValueList(Comparable v) {
    this(new OMSMRange[] {new OMSMPoint(v)});
  }

  public OMSMValueList(int[] vs) {
    this(createRange(vs));
  }

  public OMSMValueList(double[] vs) {
    this(createRange(vs));
  }

  public OMSMValueList(Comparable[] vs) {
    this(createRange(vs));
  }

  public OMSMValueList(Comparable min, Comparable max) {
    this(createRange(min, max));
  }

  private static OMSMRange[] createRange(int[] vs) {
    Comparable[] cs = new Comparable[vs.length];
    for (int i = 0; i < vs.length; i++) {
      cs[i] = new Integer(vs[i]);
    }
    return createRange(cs);
  }

  private static OMSMRange[] createRange(double[] vs) {
    Comparable[] cs = new Comparable[vs.length];
    for (int i = 0; i < vs.length; i++) {
      cs[i] = new Double(vs[i]);
    }
    return createRange(cs);
  }

  private static OMSMRange[] createRange(Comparable[] vs) {
    OMSMRange[] result = new OMSMRange[vs.length];
    for (int i = 0; i < vs.length; i++) {
      result[i] = new OMSMPoint(vs[i]);
    }
    return result;
  }

  private static OMSMRange[] createRange(Comparable min, Comparable max) {
    OMSMRange[] result = {
      new OMSMRange(min, max)
    };
    return result;
  }

  public Comparable getMax() {
    Comparable max = allowedValues[0].getMax();
    for (int i = 1; i < allowedValues.length; i++) {
      OMSMRange range = allowedValues[i];
      Comparable tmax = range.getMax();
      if (tmax.compareTo(max) > 0) max = tmax;
    }
    return max;
  }

  public Comparable getMin() {
    Comparable min = allowedValues[0].getMin();
    for (int i = 1; i < allowedValues.length; i++) {
      OMSMRange range = allowedValues[i];
      Comparable tmin = range.getMin();
      if (tmin.compareTo(min) < 0) min = tmin;
    }
    return min;
  }

  /**
   * Return a value that is the equivalent under the IN
   * ConstraintOperator to this value under the given operator. For
   * example, if the operator is LESSTHANOREQUAL, and this value is a
   * single value, the returned value will be the range from the
   * minimum value to the single value. This allows ConstraintPhrases
   * to be converted to a form that can be directly combined with
   * other ConstraintPhrases from other Plays.
   **/
  public OMSMValueList applyOperator(ConstraintOperator op) {
    if (op.equals(ConstraintOperator.GREATERTHAN)) {
      Comparable max = getMax();
      return new OMSMValueList(ComparableHelper.increment(max), ComparableHelper.getMax(max));
    }
    if (op.equals(ConstraintOperator.GREATERTHANOREQUAL)) {
      Comparable max = getMax();
      return new OMSMValueList(max, ComparableHelper.getMax(max));
    }
    if (op.equals(ConstraintOperator.LESSTHAN)) {
      Comparable min = getMin();
      return new OMSMValueList(ComparableHelper.getMin(min), ComparableHelper.decrement(min));
    }
    if (op.equals(ConstraintOperator.LESSTHANOREQUAL)) {
      Comparable min = getMin();
      return new OMSMValueList(ComparableHelper.getMin(min), min);
    }
    if (op.equals(ConstraintOperator.EQUAL)) return this;
    if (op.equals(ConstraintOperator.ASSIGN)) return this;
    if (op.equals(ConstraintOperator.NOTIN) ||
        op.equals(ConstraintOperator.NOTEQUAL)) {
      OMSMValueList newValue = complementRange(allowedValues[0]);
      for (int i = 1; i < allowedValues.length; i++) {
        newValue = newValue.intersect(complementRange(allowedValues[i]));
      }
      return newValue;
    }
    if (op.equals(ConstraintOperator.IN)) return this;
    return this;
  }

  private OMSMValueList complementRange(OMSMRange range) {
    Comparable min = range.getMin();
    Comparable max = range.getMax();
    OMSMRange[] compRange = {
      new OMSMRange(ComparableHelper.getMin(min), ComparableHelper.decrement(min)),
      new OMSMRange(ComparableHelper.increment(max), ComparableHelper.getMax(max))
    };
    return new OMSMValueList(compRange);
  }

  public OMSMValueList intersect(OMSMValueList that) {
    List result = new ArrayList(allowedValues.length);
    OMSMRange[] thatAllowedValues = that.getAllowedValues();
    for (int i = 0; i < allowedValues.length; i++) {
      Comparable thisMin = allowedValues[i].getMin();
      Comparable thisMax = allowedValues[i].getMax();
      for (int j = 0; j < thatAllowedValues.length; j++) {
        Comparable thatMin = thatAllowedValues[j].getMin();
        Comparable thatMax = thatAllowedValues[j].getMax();
        if (thatMin.compareTo(thisMax) > 0) continue; // No overlap
        if (thisMin.compareTo(thatMax) > 0) continue; // No overlap
        Comparable newMin;
        Comparable newMax;
        if (thisMin.compareTo(thatMin) < 0) {
          newMin = thatMin;
        } else {
          newMin = thisMin;
        }
        if (thisMax.compareTo(thatMax) > 0) {
          newMax = thatMax;
        } else {
          newMax = thisMax;
        }
        result.add(new OMSMRange(newMin, newMax));
      }
    }
    return new OMSMValueList((OMSMRange[]) result.toArray(new OMSMRange[result.size()]));
  }

  public Comparable getEffectiveValue() {
    return allowedValues[0].getMin();
  }

  public OMSMRange[] getAllowedValues() {
    return allowedValues;
  }

  public boolean isAllowed(Comparable v) {
    if (v.getClass() != getEffectiveValue().getClass()) return false; // Wrong class (exception?)
    for (int i = 0; i < allowedValues.length; i++) {
      if (allowedValues[i].contains(v)) {
        return true;
      }
    }
    return false;
  }

  public boolean isEmpty() {
    return allowedValues.length == 0;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append('{');
    for (int i = 0; i < allowedValues.length; i++) {
      if (i > 0) buf.append(',');
      buf.append(allowedValues[i]);
    }
    buf.append('}');
    return buf.toString();
  }

  static OMSMValueList[] v = {
    new OMSMValueList(new OMSMRange[] {
      new OMSMPoint(1.0),
      new OMSMPoint(3.0),
      new OMSMThruRange(3.5, 5.0)
    }),
    new OMSMValueList(new OMSMRange[] {
      new OMSMThruRange(2.5, 4.6)
    }),
    new OMSMValueList(new OMSMRange[] {
      new OMSMPoint(3.0),
      new OMSMPoint(5.5),
    })
  };

  public static void main(String[] args) {
    OMSMValueList o = new OMSMValueList("Abc");
    OMSMValueList x = o.applyOperator(ConstraintOperator.NOTEQUAL);
    System.out.println(o + " -> " + x);
  }
}
