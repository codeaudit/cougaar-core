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

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.AspectType;
import org.cougaar.domain.planning.ldm.plan.ScoringFunction;
import java.io.Serializable;

/**
 * Implementation of Preference.
 * @author  ALPINE <alpine-software@bbn.com>
 **/
 
public class PreferenceImpl
  implements Preference, AspectType, Cloneable, Serializable
{
  private int aspect;
  private ScoringFunction scorefun;
  private float theweight;
   
  // Default constructor
   
  public PreferenceImpl()
  {
    super();
  }
   
  /** Simple Constructor 
   * @param aspect
   * @param scoringfunction
   * @return Preference
   * @see org.cougaar.domain.planning.ldm.plan.AspectValue
   */
  
    
  public PreferenceImpl(int aspecttype, ScoringFunction scoringfunction) {
    super();
    aspect = aspecttype;
    scorefun = scoringfunction;
    theweight = (float)1.0;
  }
   
  /** Constructor that takes aspect type, scoring function and weight.
   * @param aspect
   * @param scoringfunction
   * @param weight
   * @return Preference
   * @see org.cougaar.domain.planning.ldm.plan.AspectValue
   */
  public PreferenceImpl(int aspecttype, ScoringFunction scoringfunction, double weight) {
    super();
    aspect = aspecttype;
    scorefun = scoringfunction;
    this.theweight = (float)weight;
  }

  public Object clone() {
    ScoringFunction scoringFunction = (ScoringFunction) getScoringFunction();
    return new PreferenceImpl(getAspectType(),
                              (ScoringFunction) scoringFunction.clone(),
                              getWeight());
  }
     
  //Preference interface implementations
   
  /** @return int  The AspectType that this preference represents
   * @see org.cougaar.domain.planning.ldm.plan.AspectType
   */
  public final int getAspectType() {
    return aspect;
  }
   
  /** @return ScoringFunction
   * @see org.cougaar.domain.planning.ldm.plan.ScoringFunction
   */
  public final ScoringFunction getScoringFunction() {
    return scorefun;
  }
   
  /** A Weighting of this preference from 0.0-1.0, 1.0 being high and
   * 0.0 being low.
   * @return double The weight
   */
  public final float getWeight() {
    return theweight;
  }
   
  public boolean equals(Object o) {
    if (o instanceof PreferenceImpl) {
      PreferenceImpl p = (PreferenceImpl) o;
      return aspect==p.getAspectType() &&
        theweight==p.getWeight() &&
        scorefun.equals(p.getScoringFunction());
    } else
      return false;
  }

  public int hashCode() {
    return aspect+((int)theweight*1000)+scorefun.hashCode();
  }
  public String toString() {
    return "<Preference "+aspect+" "+scorefun+" ("+theweight+")>";
  }
}
