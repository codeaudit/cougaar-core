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

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

/**
 * An EnumerationRuleParameter is a RuleParameter with specified/protected
 * string selections that returns a string
 */
public class EnumerationRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected String []my_enums;
  protected String my_value;


  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public EnumerationRuleParameter(String param_name, String []enums)
  { 
    my_enums = enums; my_value = null;
    my_name = param_name;
  }


  public EnumerationRuleParameter(String param_name)
  { 
    my_name = param_name;
    my_value = null;
  }

  public EnumerationRuleParameter() {
  }

  /**
   * Parameter type is ENUMERATION
   */
  public int ParameterType() { return RuleParameter.ENUMERATION_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public void setEnumeration(String []enums)
  { 
    my_enums = enums; 
    my_value = null;
  }

  public String[] getEnumeration() {
    return my_enums;
  }


  /**
   * Get parameter value (String)
   * @return Object parameter value (String). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be String within given list
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof String) {
      String new_string = (String)new_value;
      for(int i = 0; i < my_enums.length; i++) {
	if (my_enums[i].equals(new_string)) {
	  my_value = new_string;
	  success = true;
	  break;
	}
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.ENUMERATION_PARAMETER, 
	 "String must be in specified list : " + Enum_List());
  }

  /**
   * @param Object test_value : must be String
   * @return true if Object is a String in the enumeration, false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof String) {
      String new_string = (String)test_value;
      for(int i = 0; i < my_enums.length; i++) {
	if (my_enums[i].equals(new_string)) {
	  return true;
	}
      }
    }
    return false;
  }


  public static void Test() 
  {
    String []enums = {"First", "Second", "Third", "Fourth"};
    EnumerationRuleParameter erp = new EnumerationRuleParameter("testEnumParam", enums);

    if (erp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      erp.setValue("Fifth");
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    String s3 = "Third";
    try {
      erp.setValue(s3);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(!erp.getValue().equals(s3)) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("ERP = " + erp);
    System.out.println("EnumerationRuleParameter test complete.");

  }

  public String toString() 
  {
    return "#<ENUMERATION_PARAMETER : " + my_value + 
      " [" + Enum_List() + "] >";
  }

  public Object clone() {
    EnumerationRuleParameter erp 
      = new EnumerationRuleParameter(my_name, (String[])my_enums.clone());
    try {
      erp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return erp;
  }

  protected String Enum_List() {
    String list = "";
    for(int i = 0; i < my_enums.length; i++) {
      list += my_enums[i];
      if (i != my_enums.length-1)
	list += "/";
    }
    return list;
  }
}


