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
 * @version $Id: StringRuleParameter.java,v 1.3 2001-04-05 19:27:23 mthome Exp $
 **/

/**
 * An StringRuleParameter is a RuleParameter that returns an arbitrary string
 */
public class StringRuleParameter implements RuleParameter, SelfPrinter, java.io.Serializable {
  protected String my_name;
  protected String my_value;

  /**
   * Constructor  - Initially not set
   */
  public StringRuleParameter(String param_name) { 
    my_value = null;
    my_name = param_name;
  }

  public StringRuleParameter() {
  }

  /**
   * Parameter type is String
   */
  public int ParameterType() { return RuleParameter.STRING_PARAMETER; }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (String)
   * @returns Object parameter value (String). Note : could be null.
   */
  public Object getValue() {
    return my_value; 
  }

  /**
   * Set parameter value
   * @param Object new_value : must be String
   * @throws RuleParameterIllegalValueException (all strings accepted)
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException {
    boolean success = false;
    if (new_value instanceof String) {
      my_value = (String)new_value;
      success = true;
    }
    if (!success) 
      throw new RuleParameterIllegalValueException
	(RuleParameter.STRING_PARAMETER, "Argument must be a string.");
  }

  /**
   * @param Object test_value : must be String
   * @return true if Object is a string, false otherwise
   */
  public boolean inRange(Object test_value)
  {
    if (test_value instanceof String) {
      return true;
    }
    return false;
  }


  public String toString() {
    return "#<STRING_PARAMETER : " + my_value + ">";
  }

  public Object clone() {
    StringRuleParameter srp = new StringRuleParameter(my_name);
    try {
      srp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return srp;
  }

  public void printContent(AsciiPrinter pr) {
    pr.print(my_name, "Name");
    pr.print(my_value, "Value");
  }

}


