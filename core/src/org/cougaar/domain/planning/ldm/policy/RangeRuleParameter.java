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
 * @version $Id: RangeRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * An RangeRuleParameter is a RuleParameter with a list of 
 * integer-range delineated values and a default value. When the
 * getValue is called with the Key argument, and some value is defined
 * within some range, that value is returned. Otherwise, the default
 * is returned.
 */
public class RangeRuleParameter implements RuleParameter ,
						 java.io.Serializable
{

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public RangeRuleParameter(String param_name, RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; my_value = null;
    my_name = param_name;
  }


  public RangeRuleParameter(String param_name)
  { 
    my_name = param_name;
    my_value = null;
  }

  public void setRanges(RangeRuleParameterEntry []ranges)
  { 
    my_ranges = ranges; 
    my_value = null;
  }

  public RangeRuleParameterEntry[] getRanges() {
    return my_ranges;
  }

  /**
   * Parameter type is RANGE
   */
  public int ParameterType() { return RuleParameter.RANGE_PARAMETER; }

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
   * If key fits into one of the defined ranges, return associated
   * value, otherwise return default value (String).
   * @returns Object parameter value (String). Note : could be null.
   */
  public Object getValue(int key)
  {
      String value = my_value;
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
    if (new_value instanceof String) {
	my_value = (String)new_value;
    } else {
      throw new RuleParameterIllegalValueException
	(RuleParameter.RANGE_PARAMETER, 
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


  public static void main(String []args) 
  {
      RangeRuleParameterEntry p1 = 
	  new RangeRuleParameterEntry("LOW", 1, 3);
      RangeRuleParameterEntry p2 = 
	  new RangeRuleParameterEntry("MED", 4, 6);
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
	String value = (String)rrp.getValue(i);
	System.out.println("Value for " + i + " = " + value);
    }

    System.out.println("RRP = " + rrp);
    System.out.println("RuleRuleParameter test complete.");

  }

  public String toString() 
  {
    return "#<RANGE_PARAMETER : " + my_value + 
      " [" + Range_List() + "] >";
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

  public String getName() 
  {
    return my_name;
  }

  public Object clone() {
    RangeRuleParameter rrp 
      = new RangeRuleParameter(my_name, 
			       (RangeRuleParameterEntry[])my_ranges.clone());
    try {
      rrp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return rrp;
  }

  protected String my_name;
  protected String my_value;
  protected RangeRuleParameterEntry []my_ranges;
}
