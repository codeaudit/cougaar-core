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
 * @version $Id: DoubleRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * An DoubleRuleParameter is a RuleParameter with specified/protected
 * double bounds that returns a Double
 */
public class DoubleRuleParameter implements RuleParameter,
					    java.io.Serializable
{

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public DoubleRuleParameter(String param_name, double min, double max)
  { 
    my_min = min; my_max = max; my_value = null;
    name = param_name;
  }

  public DoubleRuleParameter(String param_name)
  { 
    name = param_name;
  }

  public void setBounds(double min, double max)
  { 
    my_min = min; 
    my_max = max;
  }

  public double getLowerBound() {
    return my_min;
  }

  public double getUpperBound() {
    return my_max;
  }



  /**
   * Parameter type is DOUBLE
   */
  public int ParameterType() { return RuleParameter.DOUBLE_PARAMETER; }

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

  public String getName() 
  {
    return name;
  }


  public Object clone() {
    DoubleRuleParameter dp = new DoubleRuleParameter(name, my_min, my_max);
    try {
      dp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return dp;
  }

  protected String name;
  protected Double my_value;
  protected double my_min;
  protected double my_max;
}
