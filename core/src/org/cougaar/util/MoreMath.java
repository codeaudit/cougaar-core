/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.util;

/**
 * Additional math functions.
 **/
public class MoreMath {

  /**
   * Returns the value ln[gamma(xx)] for xx > 0. Full accuracy is
   * obtained for xx > 1. For 0 < xx < 1, the reflection formula (6.1.4)
   * can be used first. Internal arithmetic will be done in double
   * precision, a nicety that you can omit if five figure accuracy is
   * good enough.
   * Algorithm from "Recipes for Scientific Computing"
   **/
  public static double gammaLn(double xx) {
    double x = xx - 1.0;
    double tmp = x + 5.5;
    tmp = (x + 0.5) * Math.log(tmp) - tmp;
    double ser = 1.0;
    ser += 76.18009173e0 / (x += 1.0);
    ser -= 86.50532033e0 / (x += 1.0);
    ser += 24.01409822e0 / (x += 1.0);
    ser -= 1.231739516e0 / (x += 1.0);
    ser += .120858003e-2 / (x += 1.0);
    ser -= 0.536382e-5   / (x += 1.0);
    return tmp + Math.log(2.50662827465e0 * ser);
  }
}
