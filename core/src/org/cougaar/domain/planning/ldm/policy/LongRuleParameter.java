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

package org.cougaar.domain.planning.ldm.policy;

import org.cougaar.domain.planning.ldm.policy.RuleParameter;
import org.cougaar.domain.planning.ldm.policy.RuleParameterIllegalValueException;

import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;


/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LongRuleParameter.java,v 1.4 2001-08-22 20:14:18 mthome Exp $
 **/

/**
 * An LongRuleParameter is a RuleParameter with specified/protected
 * long bounds that returns an Long
 */
public class LongRuleParameter implements RuleParameter, java.io.Serializable {

  protected String my_name;
  protected Long my_value;
  protected long my_min;
  protected long my_max;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public LongRuleParameter(String param_name, long min, long max, long value)
    throws RuleParameterIllegalValueException {
    this(param_name, min, max);
    setValue(new Long(value));
  }

  public LongRuleParameter(String param_name, long min, long max) {
    my_min = min; my_max = max; my_value = null;
    my_name = param_name;
  }

  public LongRuleParameter(String param_name) { 
    my_value = null;
    my_name = param_name;
  }

  public LongRuleParameter() {
  }

  /*
   * Parameter type is LONG
   */
  public int ParameterType() { return RuleParameter.LONG_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public long getMin() {
    return my_min;
  }
    
  public void setMin(long min) {
    my_min = min;
  }

  public long getMax() {
    return my_max;
  }

  public void setMax(long max) {
    my_max = max;
  }

  public void setBounds(long min, long max) {
    if (min > max) {
      throw new java.lang.IllegalArgumentException("min  - " + min + 
                                                   " - must be greater than max - " + max);
    }
    my_min = min; 
    my_max = max;
  }

  public long getLowerBound() {
    return getMin();
  }

  public long getUpperBound() {
    return getMax();
  }

  /**
   * Get parameter value (Long)
   * @returns Object parameter value (Long). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  public long longValue() {
    return my_value.longValue();
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Long
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Long) {
      Long new_long = (Long)new_value;
      if ((new_long.longValue() >= my_min) && 
	  (new_long.longValue() <= my_max)) {
	my_value = new_long;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.LONG_PARAMETER, 
	 "Long must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param Object test_value : must be Long
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Long) {
      Long new_long = (Long)test_value;
      if ((new_long.longValue() >= my_min) && 
	  (new_long.longValue() <= my_max))
	return true;
    }
    return false;
      
  }


  public String toString() 
  {
    return "<#LONG_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "]>";
  }


    
  public Object clone() {
    LongRuleParameter irp = new LongRuleParameter(my_name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_min, "Min");
    pr.print(my_max, "Max");
    pr.print(my_value, "Value");
  }

  public static void Test() 
  {
    LongRuleParameter irp = new LongRuleParameter("testIntParam", 3, 10);

    if (irp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      irp.setValue(new Long(11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      irp.setValue(new Long(1));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Long i4 = new Long(4);
    try {
      irp.setValue(i4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(irp.getValue() != i4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("IRP = " + irp);
    System.out.println("LongRuleParameter test complete.");

  }


}





