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
 * standing in for condition data or an operating mode and a Object
 * holding the value and a set of valid values
 */
public class ConstraintPhrase extends ConstraintOpValue implements java.io.Serializable {
  String proxyName;
  
  /**
   * Constructor 
   * @param String name of the input source, e.g., condition name
   * @param ConstraintOperator
   * @param an array of OMCRange descriptions list allowed ranges.
   */
  public ConstraintPhrase(String name, ConstraintOperator op, OMCRangeList av) {
    this(name);
    setOperator(op);
    setAllowedValues(av);
  }

  public ConstraintPhrase(String name) {
    super();
    proxyName = name;
  }
  
  /** 
   * @return The name of the condition or operating mode 
   */
  public String getProxyName() {
    return proxyName;
  }

  public String toString() {
    return proxyName + " " + super.toString();
  }
}
