/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
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
