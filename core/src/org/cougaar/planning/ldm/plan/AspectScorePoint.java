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

package org.cougaar.planning.ldm.plan;

import java.io.Serializable;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 *
 */
 
public class AspectScorePoint implements Serializable, Cloneable {
  private AspectValue value;
  private double score;

  public AspectScorePoint(AspectValue value, double score) {
    this.value = value;
    this.score = score;
  }

  /** @deprecated Use AspectScorePoint(AspectValue,double) instead **/
  public AspectScorePoint(double value, double score, int type) {
    this.value = AspectValue.newAspectValue(type,value);
    this.score = score;
  }

  public Object clone() {
    return new AspectScorePoint(value, score);
  }

  /* @return double The 'score'.
   */
  public double getScore() { return score; }
   
  /* @return Aspect The value and type of aspect.
   * @see org.cougaar.planning.ldm.plan.AspectValue
   */
  public AspectValue getAspectValue() { return value; }
   
  public double getValue() { return value.getValue(); }
  public int getAspectType() { return value.getAspectType(); }

  public static final AspectScorePoint getNEGATIVE_INFINITY(int type) {
    return new AspectScorePoint(0.0, Double.NEGATIVE_INFINITY, type);
  }
  public static final AspectScorePoint getPOSITIVE_INFINITY(int type) {
    return new AspectScorePoint(0, Double.POSITIVE_INFINITY, type);
  }

}
