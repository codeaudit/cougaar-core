/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

/** 
 * The Play determines what values to apply to a circumstance.
 */
public class PlayBase {

  private ConstrainingClause ifClause;
  private ConstraintPhrase[] operatingModeConstraints;

  /**
   * Constructor
   * @param ConstraingClause representing the 'if' clause
   * @param ConstraingClause representing the 'then' clause
   **/
  public PlayBase(ConstrainingClause ifClause, ConstraintPhrase[] omConstraints) {
    this.ifClause = ifClause;
    this.operatingModeConstraints = omConstraints;
  }

  /** 
   * A comparison based on sensor data and operating modes 
   * @return the 'if' ConstrainingClause 
   */
  public ConstrainingClause getIfClause() {
    return ifClause;
  }

  /**
   * Knobs with current setting and a range or enumeration of
   * allowable settings for this play in order of desirability. 
   * Should "then" clause be limited to && expressions?
   * @return 'then' ConstrainingClause 
   */ 
  public ConstraintPhrase[] getOperatingModeConstraints() {
    return operatingModeConstraints;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(ifClause);
    for (int i = 0; i < operatingModeConstraints.length; i++) {
      buf.append(":")
        .append(operatingModeConstraints[i]);
    }
    return buf.toString();
  }
}
