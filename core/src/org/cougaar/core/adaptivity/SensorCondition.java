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
import java.util.*;

/** 
 * A class to hold a sensor condition. Most sensors will subclass
 * this class to add a private or package protected setValue method to
 * preclude arbitrary classes from messing with the value.
 */
public class SensorCondition extends OMCBase implements Condition {
  /**
   * Constructor with no specified initial value. The initial value is
   * set to the effective value of the allowedValues.
   * @param name the name of this SensorCondition
   * @param allowedValues the list of allowed value ranges
   **/
  public SensorCondition(String name, OMCRangeList allowedValues) {
    super(name, allowedValues, allowedValues.getEffectiveValue());
  }
  /**
   * Constructor with a specified initial value.
   * @param name the name of this SensorCondition
   * @param allowedValues the list of allowed value ranges
   * @param initialValue the initial value of this OperatingMode
   **/
  public SensorCondition(String name, OMCRangeList allowedValues, Comparable initialValue) {
    super(name, allowedValues, initialValue);
  }
}
