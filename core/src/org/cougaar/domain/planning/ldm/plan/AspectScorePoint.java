/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.io.Serializable;

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectScorePoint.java,v 1.2 2001-04-05 19:27:13 mthome Exp $
 */
 
public class AspectScorePoint implements Serializable, Cloneable {
  private AspectValue value;
  private double score;

  public AspectScorePoint(AspectValue value, double score) {
    this.value = value;
    this.score = score;
  }

  public AspectScorePoint(double value, double score) {
    this.value = new AspectValue(0,value);
    this.score = score;
  }

  public Object clone() {
    return new AspectScorePoint((AspectValue) value.clone(), score);
  }

  /* @return double The 'score'.
   */
  public double getScore() { return score; }
   
  /* @return Aspect The value and type of aspect.
   * @see org.cougaar.domain.planning.ldm.plan.AspectValue
   */
  public AspectValue getAspectValue() { return value; }
   
  public double getValue() { return value.getValue(); }
  public int getAspectType() { return value.getAspectType(); }

  public static final AspectScorePoint NEGATIVE_INFINITY = 
    new AspectScorePoint(0, Double.NEGATIVE_INFINITY);
  public static final AspectScorePoint POSITIVE_INFINITY = 
    new AspectScorePoint(0, Double.POSITIVE_INFINITY);

}
