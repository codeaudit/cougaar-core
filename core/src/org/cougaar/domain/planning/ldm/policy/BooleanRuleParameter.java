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
 * @version $Id: BooleanRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * A BooleanRuleParameter is a RuleParameter that contains a single true
 * or false value
 */
public class BooleanRuleParameter implements RuleParameter,
					    java.io.Serializable
{

  /**
   * Constructor  - Initially not set
   */
  public BooleanRuleParameter(String param_name)
  { 
    my_value = null;
    name = param_name;
  }

  /**
   * Constructor with value set
   */
  public BooleanRuleParameter(String param_name, boolean value)
  { 
    my_value = new Boolean(value);
    name = param_name;
  }

  /**
   * Parameter type is Boolean
   */
  public int ParameterType() { return RuleParameter.BOOLEAN_PARAMETER; }

  /**
   * Get parameter value (Boolean)
   * @returns Object parameter value (Boolean). Note : could be null.
   */
  public Object getValue()
  {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Boolean
   * @throws RuleParameterIllegalValueException (only Boolean accepted)
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
    boolean success = false;
    if (new_value instanceof Boolean) {
      my_value = (Boolean)new_value;
      success = true;
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.BOOLEAN_PARAMETER, "Argument must be a Boolean.");
  }

  /**
   * @param Object test_value : must be Boolean
   * @return true if Object is a Boolean, false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof Boolean) {
      return true;
    }
    return false;
  }


  public String toString() 
  {
    return "#<BOOLEAN_PARAMETER : " + my_value + ">";
  }

  public String getName() 
  {
    return name;
  }

  public Object clone() {
    BooleanRuleParameter brp = new BooleanRuleParameter(name);
    try {
      brp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return brp;
  }


  protected String name;
  protected Boolean my_value;
}
