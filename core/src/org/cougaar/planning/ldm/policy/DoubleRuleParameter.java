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

package org.cougaar.planning.ldm.policy;

import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;

import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

/**
 * An DoubleRuleParameter is a RuleParameter with specified/protected
 * double bounds that returns a Double
 */
public class DoubleRuleParameter implements RuleParameter, SelfPrinter, java.io.Serializable {
  protected String my_name;
  protected double my_min;
  protected double my_max;
  protected Double my_value;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public DoubleRuleParameter(String param_name, double min, double max)
  { 
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public DoubleRuleParameter(String param_name)
  { 
    my_name = param_name;
  }

  public DoubleRuleParameter() {
  }

  /**
   * Parameter type is DOUBLE
   */
  public int ParameterType() { return RuleParameter.DOUBLE_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public double getMin() {
    return my_min;
  }
    
  public void setMin(double min) {
    my_min = min;
  }

  public double getMax() {
    return my_max;
  }

  public void setMax(double max) {
    my_max = max;
  }

  public void setBounds(double min, double max) { 
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public double getLowerBound() {
    return getMin();
  }

  public double getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Double)
   * @returns Object parameter value (Double). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Double
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Double) {
      Double new_double = (Double)new_value;
      if ((new_double.intValue() >= my_min) && 
	  (new_double.intValue() <= my_max)) {
	my_value = new_double;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.DOUBLE_PARAMETER, 
	 "Double must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param Object test_value : must be Double
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Double) {
      Double new_double = (Double)test_value;
      if ((new_double.doubleValue() >= my_min) && 
	  (new_double.doubleValue() <= my_max))
	return true;
    }
    return false;
      
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_min, "Min");
    pr.print(my_max, "Max");
    pr.print(my_value, "Value");
  }

  public static void Test() 
  {
    DoubleRuleParameter drp = new DoubleRuleParameter("testDoubleParam", 3.14, 10.73);

    if (drp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      drp.setValue(new Double(11.11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      drp.setValue(new Double(1.2));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Double d4 = new Double(4.5);
    try {
      drp.setValue(d4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(drp.getValue() != d4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("DRP = " + drp);
    System.out.println("DoubleRuleParameter test complete.");

  }

  public String toString() 
  {
    return "#<DOUBLE_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }

  public Object clone() {
    DoubleRuleParameter dp = new DoubleRuleParameter(my_name, my_min, my_max);
    try {
      dp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return dp;
  }


}
