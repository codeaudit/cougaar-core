/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.io.Serializable;

/** 
 * Base class for Plays and OperatingModePolicies. Most of the
 * functionality of the subclasses resides here. A Play or
 * OperatingModePolicy has a ConstrainingClause that can be evaluated
 * to determine if it applies to the current Conditions and a list
 * (array) of operating mode constraints that specify the values that
 * the modes should be given.
 **/
public class PlayBase implements java.io.Serializable {

  private ConstrainingClause ifClause;
  private ConstraintPhrase[] operatingModeConstraints;

  /**
   * Constructor
   * @param ifClause the 'if' clause
   * @param omConstraints the constraints on operating modes
   **/
  public PlayBase(ConstrainingClause ifClause, ConstraintPhrase[] omConstraints) {
    this.ifClause = ifClause;
    this.operatingModeConstraints = omConstraints;
  }

  /** 
   * Gets the if clause
   * @return the 'if' ConstrainingClause
   */
  public ConstrainingClause getIfClause() {
    return ifClause;
  }

  /**
   * Gets the array of ConstraintPhrases to be applied to the
   * operating modes.
   * @return the array of ConstraintPhrases.
   **/
  public ConstraintPhrase[] getOperatingModeConstraints() {
    return operatingModeConstraints;
  }

  /**
   * Gets the Play or OperatingModePolicy as a String. The form of the
   * String is approximately the same as the input to the Parser.
   * @return The Play or OperatingModePolicy as a String.
   **/
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(ifClause);
    for (int i = 0; i < operatingModeConstraints.length; i++) {
      buf.append(":")
        .append(operatingModeConstraints[i]);
    }
    return buf.toString();
  }

  public int hashCode() {
    int hc = ifClause.hashCode();
    for (int i = 0; i < this.operatingModeConstraints.length; i++) {
      hc = 31 * hc + operatingModeConstraints[i].hashCode();
    }
    return hc;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (this.getClass() != o.getClass()) return false;
    PlayBase that = (PlayBase) o;
    if (!this.ifClause.equals(that.ifClause)) return false;
    if (this.operatingModeConstraints.length != that.operatingModeConstraints.length) return false;
    for (int i = 0; i < this.operatingModeConstraints.length; i++) {
      if (!this.operatingModeConstraints[i].equals(that.operatingModeConstraints[i])) return false;
    }
    return true;
  }
}
