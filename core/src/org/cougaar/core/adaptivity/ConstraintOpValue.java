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
 * A phrase used to express a boolean comparison between a string
 * standing in for sensor data or an operating mode and a Object
 * holding the value and a set of valid values
 */
public class ConstraintOpValue {
  ConstraintOperator operator;
  OMSMValueList allowedValues;
  
  /**
   * Constructor 
   * @param String name of the input source, e.g., sensor name
   * @param ConstraintOperator
   * @param an array of OMSMRange descriptions list allowed ranges.
   */
  public ConstraintOpValue() {
  }

  public void setOperator(ConstraintOperator op) {
    operator = op;
  }

  public void setAllowedValues(OMSMValueList l) {
    allowedValues = l;
  }
  
  /** 
   * Get the effective value of the allowed values. This is always the
   * the min of the first range.
   * @return the value as a Comparable (String, Integer, Double, etc.)
   **/
  public Comparable getValue() {
    return allowedValues.getEffectiveValue();
  }

  /**
   * Get the ranges of allowed values.
   * @return all allowed ranges as imposed by this constraint
   **/
  public OMSMValueList getAllowedValues() {
    return allowedValues;
  }
  
  /** 
   * The relationship between the sensor or operating mode and the
   * value.
   * @return ConstraintOperator */
  public ConstraintOperator getOperator() {
    return operator;
  }

  public String toString() {
    return operator + " " + allowedValues;
  }
}
