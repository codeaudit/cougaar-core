package org.cougaar.core.adaptivity;

import java.util.Map;
import java.util.HashMap;

/**
 * Helper methods for dealing with Comparable values.
 **/
public class ComparableHelper {
  /**
   * Maps the class of the Comparable to a minimum value for that type
   **/
  private static Map minimumValue = new HashMap();

  /**
   * Maps the class of the Comparable to a maximum value for that type
   **/
  private static Map maximumValue = new HashMap();

  /**
   * A String have the "maximum" value. Obviously, there is no true
   * maximum, but this String should be greater than any String likely
   * to encountered in the US.
   **/
  private static final String ffffString = "\uffff";

  /**
   * Initialize the minimumValue and maximumValue Maps.
   **/
  static {
    maximumValue.put(String.class, "\uffff");
    maximumValue.put(Double.class, new Double(Double.MAX_VALUE));
    maximumValue.put(Float.class, new Float(Float.MAX_VALUE));
    maximumValue.put(Long.class, new Long(Long.MAX_VALUE));
    maximumValue.put(Integer.class, new Integer(Integer.MAX_VALUE));
    minimumValue.put(String.class, "");
    minimumValue.put(Double.class, new Double(Double.MIN_VALUE));
    minimumValue.put(Float.class, new Float(Float.MIN_VALUE));
    minimumValue.put(Long.class, new Long(Long.MIN_VALUE));
    minimumValue.put(Integer.class, new Integer(Integer.MIN_VALUE));
  };

  /**
   * Get a value of the same type as the supplied value having the
   * maximum value for that type. This is not possible, in general,
   * but we approximate the values where necessary.
   **/
  public static Comparable getMax(Comparable v) {
    return (Comparable) maximumValue.get(v.getClass());
  }

  /**
   * Get a value of the same type as the supplied value having the
   * minimum value for that type. This is not possible, in general,
   * but we approximate the values where necessary.
   **/
  public static Comparable getMin(Comparable v) {
    return (Comparable) minimumValue.get(v.getClass());
  }

  /**
   * Get a value of the same type as the supplied value having the
   * next greater value for that type. Not implemented for all
   * Comparables.
   **/
  public static Comparable increment(Comparable v) {
    Class vClass = v.getClass();
    if (vClass == String.class) return ((String) v) + "\0000";
    if (vClass == Double.class) {
      double d = ((Double) v).doubleValue();
      return new Double(incrementDouble(d));
    }
    if (vClass == Integer.class) return new Integer(((Integer) v).intValue() + 1);
    return v;
  }

  /**
   * Get a value of the same type as the supplied value having the
   * next smaller value for that type. Not implemented for all
   * Comparables.
   **/
  public static Comparable decrement(Comparable v) {
    Class vClass = v.getClass();
    if (vClass == String.class) return decrementString((String) v);
    if (vClass == Double.class) {
      double d = ((Double) v).doubleValue();
      return new Double(-incrementDouble(-d));
    }
    if (vClass == Integer.class) return new Integer(((Integer) v).intValue() - 1);
    return v;
  }

  /**
   * Decrementing a String is impossible (it would be infinitely
   * long). We approximate by returning a String of the same length.
   **/
  private static String decrementString(String s) {
    int l = s.length();
    char lastChar = s.charAt(l - 1);
    String head = s.substring(0, l - 1);
    String tail;
    if (lastChar == '\u0000') {
      head = decrementString(head);
      tail = "\uffff";
    } else {
      tail = new String(new char[] {(char) (lastChar - 1)});
    }
    return head + tail;
  }

  /**
   * Increment a double to the next representable value. The result is
   * such that there is no representation for any values between the
   * given value and the result.
   **/
  private static double incrementDouble(double v) {
    long bits = Double.doubleToLongBits(v);
    long m = (bits & 0xfffffffffffffL); // The significand
    long s = bits & 0x8000000000000000L; // The sign
    int e = (int)((bits >> 52) & 0x7ffL); // The exponent
    if (s == 0L) {              // Positive numbers
      if (m == 0x000fffffffffffffL) {
        m = 0;                  // Carry results in 0
        e++;                    // and increments the exponent
      } else {
        m += 1L;                // Carry is not a problem, just increment
      }
    } else {                    // Negative numbers
      if (m == 0x0000000000000000L) {
        if (e > 0) {
          m = 0x000fffffffffffffL; // Borrow results in all ones
          e--;                  // and decrements the exponent
        } else {                // Negative zero shouldn't happen
          m = 1;                // But, return smallest
          s = 0;                // positive non-zero value
          e = 0;                // if it does
        }
      } else {                  // Simple case
        m -= 1L;                // Just decrement the significand
        if (m == 0L && e == 0) {
          s = 0;                // Avoid negative zero.
        }
      }
    }
    bits = s | (((long) e) << 52) | m;
    return Double.longBitsToDouble(bits);
  }

  /**
   * Tests (should be moved to regress)
   **/
  public static void main(String[] args) {
    long[] testBits = {
      0x0010000000000000L,
      0x0008000000000000L,
      0x0004000000000000L,
      0x0002000000000000L,
      0x001fffffffffffffL,
      0x000fffffffffffffL,
      0x0007ffffffffffffL,
      0x0003ffffffffffffL,
    };
    for (int i = 0; i < testBits.length; i++) {
      double v = Double.longBitsToDouble(testBits[i]);
      double iv = incrementDouble(v);
      double av = (v + iv) * 0.5;
      System.out.println("    v=" + v);
      System.out.println("   iv=" + iv);
      System.out.println("   av=" + av);
      System.out.println(" v<iv=" + (v<iv));
      System.out.println(" v<av=" + (v<av));
      System.out.println("av<iv=" + (av<iv));
    }
    System.out.println("decrement(A)=" + decrement("A"));
    System.out.println("increment(A)=" + increment("A"));
  }
}
