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
 * @version $Id: IntegerRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * An IntegerRuleParameter is a RuleParameter with specified/protected
 * integer bounds that returns an Integer
 */
public class IntegerRuleParameter implements RuleParameter, 
					     java.io.Serializable
{

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public IntegerRuleParameter(String param_name, int min, int max)
  { 
    my_min = min; my_max = max; my_value = null;
    name = param_name;
  }

  public IntegerRuleParameter(String param_name)
  { 
    my_value = null;
    name = param_name;
  }

  public void setBounds(int min, int max) {
    my_min = min; 
    my_max = max;
  }

  public int getLowerBound() {
    return my_min;
  }

  public int getUpperBound() {
    return my_max;
  }

  /**
   * Parameter type is INTEGER
   */
  public int ParameterType() { return RuleParameter.INTEGER_PARAMETER; }

  /**
   * Get parameter value (Integer)
   * @returns Object parameter value (Integer). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Integer
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Integer) {
      Integer new_integer = (Integer)new_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max)) {
	my_value = new_integer;
	success = true;
      }
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.INTEGER_PARAMETER, 
	 "Integer must be between " + my_min + " and " + my_max);
  }

  /**
   * 
   * @param Object test_value : must be Integer
   * @return true if test_value is within the acceptable range
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Integer) {
      Integer new_integer = (Integer)test_value;
      if ((new_integer.intValue() >= my_min) && 
	  (new_integer.intValue() <= my_max))
	return true;
    }
    return false;
      
  }

  public static void Test() 
  {
    IntegerRuleParameter irp = new IntegerRuleParameter("testIntParam", 3, 10);

    if (irp.getValue() != null) {
      System.out.println("Error : Parameter not initialized to null");
    }
    
    try {
      irp.setValue(new Integer(11));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    try {
      irp.setValue(new Integer(1));
      System.out.println("Error detecting illegal set condition");
    } catch(RuleParameterIllegalValueException rpive) {
    }

    Integer i4 = new Integer(4);
    try {
      irp.setValue(i4);
    } catch(RuleParameterIllegalValueException rpive) {
      System.out.println("Error detecting legal set condition");
    }

    if(irp.getValue() != i4) {
      System.out.println("Error retrieving value of parameter");
    }

    System.out.println("IRP = " + irp);
    System.out.println("IntegerRuleParameter test complete.");

  }

  public String toString() 
  {
    return "#<INTEGER_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }


  public String getName() 
  {
    return name;
  }

  public Object clone() {
    IntegerRuleParameter irp = new IntegerRuleParameter(name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  protected String name;
  protected Integer my_value;
  protected int my_min;
  protected int my_max;
}
