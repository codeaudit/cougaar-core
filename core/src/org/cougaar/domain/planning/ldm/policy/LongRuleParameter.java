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
 * @version $Id: LongRuleParameter.java,v 1.1 2001-02-15 19:40:05 tomlinso Exp $
 **/

/**
 * An LongRuleParameter is a RuleParameter with specified/protected
 * long bounds that returns an Long
 */
public class LongRuleParameter implements RuleParameter, 
					     java.io.Serializable
{

  /**
   * Constructor sets min/max values and establishes value as not set
   */
  public LongRuleParameter(String param_name, long min, long max, long value)
    throws RuleParameterIllegalValueException
  {
    this(param_name, min, max);
    setValue(new Long(value));
  }

  public LongRuleParameter(String param_name, long min, long max) {
    my_min = min; my_max = max; my_value = null;
    name = param_name;
  }

  public LongRuleParameter(String param_name)
  { 
    my_value = null;
    name = param_name;
  }

  public void setBounds(long min, long max) {
    my_min = min; 
    my_max = max;
  }

  public long getLowerBound() {
    return my_min;
  }

  public long getUpperBound() {
    return my_max;
  }

  /**
   * Parameter type is LONG
   */
  public int ParameterType() { return RuleParameter.LONG_PARAMETER; }

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

  public String toString() 
  {
    return "#<LONG_PARAMETER : " + my_value + 
      " [" + my_min + " , " + my_max + "] >";
  }


  public String getName() 
  {
    return name;
  }

  public Object clone() {
    LongRuleParameter irp = new LongRuleParameter(name, my_min, my_max);
    try {
      irp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return irp;
  }

  protected String name;
  protected Long my_value;
  protected long my_min;
  protected long my_max;
}
