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
 * @version $Id: StringRuleParameter.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/

/**
 * An StringRuleParameter is a RuleParameter that returns an arbitrary string
 */
public class StringRuleParameter implements RuleParameter,
					    java.io.Serializable
{

  /**
   * Constructor  - Initially not set
   */
  public StringRuleParameter(String param_name)
  { 
    my_value = null;
    name = param_name;
  }

  /**
   * Parameter type is String
   */
  public int ParameterType() { return RuleParameter.STRING_PARAMETER; }

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
   * @param Object new_value : must be String
   * @throws RuleParameterIllegalValueException (all strings accepted)
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException
  {
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


  public String toString() 
  {
    return "#<STRING_PARAMETER : " + my_value + ">";
  }

  public String getName() 
  {
    return name;
  }

  public Object clone() {
    StringRuleParameter srp = new StringRuleParameter(name);
    try {
      srp.setValue(my_value);
    } catch(RuleParameterIllegalValueException rpive) {}
    return srp;
  }


  protected String name;
  protected String my_value;
}
