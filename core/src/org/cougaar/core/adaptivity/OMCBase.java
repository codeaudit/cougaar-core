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
import org.cougaar.planning.ldm.policy.RuleParameter;

/**
 * The base class for OperatingMode and Condition implementations.
 * These two concepts differ only in that an OperatingMode can be set
 * by the AdaptivityEngine whereas a Condition is under the
 * control of the component establishing the Condition.
 **/
public class OMCBase implements java.io.Serializable {
  private String name;
  private Comparable value;
  private OMCRangeList allowedValues;

  /**
   * Constructor using default initial value.
   * @param name the name of this
   * @param allowedValues the allowed value ranges. It is illegal to
   * set a value that is not in the allowed ranges.
   **/
  protected OMCBase(String name, OMCRangeList allowedValues) {
    this.name = name;
    this.allowedValues = allowedValues;
    this.value = allowedValues.getEffectiveValue();
  }

  /**
   * Constructor with a specific initialvalue.
   * @param name the name of this
   * @param allowedValues the allowed value ranges. It is illegal to
   * set a value that is not in the allowed ranges.
   * @param initialValue the initial value. Must not be null. All
   * subsequent values that are set for this must have the same class
   * as the initial value.
   **/
  protected OMCBase(String name, OMCRangeList allowedValues, Comparable initialValue) {
    this(name, allowedValues);
    setValue(initialValue);
  }

  /**
   * Gets the name
   * @return the name
   **/
  public String getName() {
    return name;
  }

  /**
   * Gets the list if allowed value ranges. The allowed value ranges
   * are set in the constructor and immutable thereafter.
   * @return the list of allowed value ranges.
   **/
  public OMCRangeList getAllowedValues() {
    return allowedValues;
  }

  /**
   * Gets the current value
   * @return the current value
   **/
  public Comparable getValue() {
    return value;
  }

  /**
   * Set a new value for this. The new value must be in the list of
   * allowed value ranges. This method is protected because the
   * ability to set a new value must be under the control of a
   * responsible component. Subclasses will override this with public
   * or package protected versions as needed.
   * @param newValue the new value. Must be an allowed value.
   **/
  protected void setValue(Comparable newValue) {
    if (!allowedValues.isAllowed(newValue)) {
      throw new IllegalArgumentException("setValue: " + getName() + " = " + newValue + " not allowed");
    }
    if (value.compareTo(newValue) == 0) return; // Already set to this value
    Comparable oldValue = getValue();
    value = newValue;
  }

  /**
   * Furnish a useful string representation.
   * @return the name and value separated by an equals sign.
   **/
  public String toString() {
    return getName() + " = " + getValue();
  }
}
