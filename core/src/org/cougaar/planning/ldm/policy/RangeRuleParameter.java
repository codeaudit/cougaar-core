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
 * An RangeRuleParameter is a RuleParameter with a list of 
 * integer-range delineated values and a default value. When the
 * getValue is called with the Key argument, and some value is defined
 * within some range, that value is returned. Otherwise, the default
 * is returned.
 */
public class RangeRuleParameter implements RuleParameter, SelfPrinter, 
  java.io.Serializable {
  protected String my_name;
  protected RangeRuleParameterEntry []my_ranges;
  protected Object my_default_value;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public RangeRuleParameter(String param_name, RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; 
    my_default_value = null;
    my_name = param_name;
  }


  public RangeRuleParameter(String param_name)
  { 
    my_name = param_name;
    my_default_value = null;
  }

  public RangeRuleParameter() {
  }

  /**
   * Parameter type is RANGE
   */
  public int ParameterType() { return RuleParameter.RANGE_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public RangeRuleParameterEntry[] getRanges() {
    return my_ranges;
  }

  public void setRanges(RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; 
    my_default_value = null;
  }

  /**
   * Get parameter value
   * @return parameter value. Note : could be null.
   */
  public Object getValue()
  {
    return my_default_value; 
  }

  /**
   * Get parameter value keyed by int
   * If key fits into one of the defined ranges, return associated
   * value, otherwise return default value.
   * @return parameter value. Note : could be null.
   */
  public Object getValue(int key)
  {
      Object value = my_default_value;
      for(int i = 0; i < my_ranges.length; i++) {
	  if ((my_ranges[i].getRangeMin() <= key) &&
	      (my_ranges[i].getRangeMax() >= key)) {
	      value = my_ranges[i].getValue();
	      break;
	  }
      }

    return value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be String within given list
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    my_default_value = new_value;
  }

  /**
   * @param Object test_value 
   * @return always returns true 
   */
  public boolean inRange(Object test_value)
  {
    return true;
  }

  public String toString() 
  {
    return "#<RANGE_PARAMETER : " + my_default_value + 
      " [" + Range_List() + "] >";
  }

  public Object clone() {
    RangeRuleParameter rrp = new RangeRuleParameter(my_name);
    if (my_ranges != null) {
      rrp.setRanges((RangeRuleParameterEntry[])my_ranges.clone());
    }
    try {
      rrp.setValue(my_default_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return rrp;
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_ranges, "Ranges");
    pr.print(my_default_value, "Value");
  }
  
  public static void main(String []args) {
    RangeRuleParameterEntry p1 = 
      new RangeRuleParameterEntry("LOW", 1, 3);
    RangeRuleParameterEntry p2 = 
      new RangeRuleParameterEntry(new Integer(37), 4, 6);
    RangeRuleParameterEntry p3 = 
      new RangeRuleParameterEntry("HIGH", 7, 9);
    
    RangeRuleParameterEntry []ranges = {p1, p2, p3};
    RangeRuleParameter rrp = 
      new RangeRuleParameter("testRangeParam", ranges);
    
    if (rrp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      rrp.setValue("DFLT");
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting illegal set condition");
    }

    for(int i = 0; i <= 10; i++) {
      Object value = rrp.getValue(i);
      System.out.println("Value for " + i + " = " + value);
    }
    
    System.out.println("RRP = " + rrp);
    System.out.println("RuleRuleParameter test complete.");
    
  }
  
  protected String Range_List() {
    String list = "";
    for(int i = 0; i < my_ranges.length; i++) {
      list += my_ranges[i];
      if (i != my_ranges.length-1)
	list += "/";
    }
    return list;
  }
}


