/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.measure;

import java.io.Serializable;

/** Base (abstract) implementation of all Measure classes.
 * @see Measure for specification.
 **/

public abstract class AbstractMeasure implements Measure {
  
  public static AbstractMeasure newMeasure(String s, int unit) {
    throw new UnknownUnitException();
  }

  /** given a string like "100 meters", find the index of the 'm' 
   * in the units.  Will search for the first of either the char 
   * after a space or the first letter found.
   * if a likely spot is not found, return -1.
   **/
  protected final static int indexOfType(String s) {
    int l = s.length();
    for (int i = 0; i < l; i++) {
      char c = s.charAt(i);
      if (c == ' ') return i+1;
      if (Character.isLetter(c)) return i;
    }
    return -1;
  }

}
