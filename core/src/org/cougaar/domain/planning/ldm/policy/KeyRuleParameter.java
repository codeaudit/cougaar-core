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


import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;


/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: KeyRuleParameter.java,v 1.4 2001-03-21 16:14:20 ngivler Exp $
 **/

/**
 * An KeyRuleParameter is a RuleParameter with a list of 
 * key/value pairs and a default value. When the
 * getValue is called with the Key argument, and some value is defined
 * for that key, that value is returned. Otherwise, the default
 * is returned.
 */
public class KeyRuleParameter implements RuleParameter, SelfPrinter, java.io.Serializable {

  protected String my_name;
  protected KeyRuleParameterEntry []my_keys;
  protected String my_value;

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public KeyRuleParameter(String param_name, KeyRuleParameterEntry []keys)
  { 
    my_keys = keys; my_value = null;
    my_name = param_name;
  }


  public KeyRuleParameter(String param_name)
  { 
    my_name = param_name;
    my_value = null;
  }

  public KeyRuleParameter() {
  }

  /**
   * Parameter type is KEY
   */
  public int ParameterType() { return RuleParameter.KEY_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  public KeyRuleParameterEntry[] getKeys() {
    return my_keys;
  }

  public void setKeys(KeyRuleParameterEntry []keys)
  { 
    my_keys = keys; 
    my_value = null;
  }


  /**
   * Get parameter value (String)
   * @returns Object parameter value (String). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Get parameter value (String) keyed by int
   * If key equals one of the defined keys, return associated
   * value, otherwise return default value (String).
   * @returns Object parameter value (String). Note : could be null.
   */
  public Object getValue(String key)
  {
      String value = my_value;
      for(int i = 0; i < my_keys.length; i++) {
	  if (my_keys[i].getKey().equals(key)) {
	      value = my_keys[i].getValue();
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
    if (new_value instanceof String) {
	my_value = (String)new_value;
    } else {
      throw new RuleParameterIllegalValueException
	(RuleParameter.KEY_PARAMETER, 
	 "String must be specified");
    }
  }

  /**
   * @param Object test_value : must be String
   * @return true if Object is a String in the enumeration, false otherwise
   */
  public boolean inRange(Object test_value)
  {
      return (test_value instanceof String);
  }

  public Object clone() {
    KeyRuleParameter krp = new KeyRuleParameter(my_name);
    if (my_keys != null) {
      krp.setKeys((KeyRuleParameterEntry[])my_keys.clone());
    }
    try {
      krp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return krp;
  }


  
  public String toString() 
  {
    return "#<KEY_PARAMETER : " + my_value + 
      " [" + Key_List() + "] >";
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_keys, "Keys");
    pr.print(my_value, "Value");
  }

  protected String Key_List() {
    String list = "";
    for(int i = 0; i < my_keys.length; i++) {
      list += my_keys[i];
      if (i != my_keys.length-1)
	list += "/";
    }
    return list;
  }

  public static void main(String []args) 
  {
      KeyRuleParameterEntry p1 = 
	  new KeyRuleParameterEntry("LOW", "AAA");
      KeyRuleParameterEntry p2 = 
	  new KeyRuleParameterEntry("MED", "BBB");
      KeyRuleParameterEntry p3 = 
	  new KeyRuleParameterEntry("HIGH", "CCC");

    KeyRuleParameterEntry []keys = {p1, p2, p3};
    KeyRuleParameter krp = 
	new KeyRuleParameter("testKeyParam", keys);

    if (krp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      krp.setValue("DFLT");
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting illegal set condition");
    }

    String []tests = {"NONE", "LOW", "HIGH", "MED", "DUMMY"};
    for (int i = 0; i < tests.length; i++) {
	System.out.println("Value for " + tests[i] + " = " + 
			   krp.getValue(tests[i]));
    }

    System.out.println("KRP = " + krp);
    System.out.println("KeyRuleParameter test complete.");

  }

}




