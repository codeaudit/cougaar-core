/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.domain.planning.ldm.policy;


import org.cougaar.domain.planning.ldm.policy.RuleParameter;
import org.cougaar.domain.planning.ldm.policy.RuleParameterIllegalValueException;

import org.cougaar.core.util.AsciiPrinter;
import org.cougaar.core.util.SelfPrinter;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: BooleanRuleParameter.java,v 1.3 2001-04-05 19:27:22 mthome Exp $
 **/

/**
 * A BooleanRuleParameter is a RuleParameter that contains a single true
 * or false value
 */
public class BooleanRuleParameter implements RuleParameter, SelfPrinter, java.io.Serializable {
  protected String my_name;
  protected Boolean my_value;

  /**
   * Constructor  - Initially not set
   */
  public BooleanRuleParameter(String param_name)
  { 
    my_value = null;
    my_name = param_name;
  }

  /**
   * Constructor with value set
   */
  public BooleanRuleParameter(String param_name, boolean value)
  { 
    my_value = new Boolean(value);
    my_name = param_name;
  }

  public BooleanRuleParameter() {
  }

  /**
   * Parameter type is Boolean
   */
  public int ParameterType() { return RuleParameter.BOOLEAN_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

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

  public Object clone() {
    BooleanRuleParameter brp = new BooleanRuleParameter(my_name);
    try {
      brp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return brp;
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_value, "Value");
  }

}


