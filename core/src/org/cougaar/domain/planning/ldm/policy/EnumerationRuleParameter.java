/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.policy;

import org.cougaar.domain.planning.ldm.policy.RuleParameter;
import org.cougaar.domain.planning.ldm.policy.RuleParameterIllegalValueException;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: EnumerationRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * An EnumerationRuleParameter is a RuleParameter with specified/protected
 * string selections that returns a string
 */
public class EnumerationRuleParameter implements RuleParameter ,
						 java.io.Serializable
{

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public EnumerationRuleParameter(String param_name, String []enums)
  { 
    my_enums = enums; my_value = null;
    name = param_name;
  }


  public EnumerationRuleParameter(String param_name)
  { 
    name = param_name;
    my_value = null;
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
   * Parameter type is ENUMERATION
   */
  public int ParameterType() { return RuleParameter.ENUMERATION_PARAMETER; }

  /**
   * Get parameter value (String)
   * @returns Object parameter value (String). Note : could be null.
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

  protected String Enum_List() {
    String list = "";
    for(int i = 0; i < my_enums.length; i++) {
      list += my_enums[i];
      if (i != my_enums.length-1)
	list += "/";
    }
    return list;
  }

  public String getName() 
  {
    return name;
  }

  public Object clone() {
    EnumerationRuleParameter erp 
      = new EnumerationRuleParameter(name, (String[])my_enums.clone());
    try {
      erp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return erp;
  }

  protected String name;
  protected String my_value;
  protected String []my_enums;
}
