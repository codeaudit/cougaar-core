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

 /** 
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: RuleParameterIllegalValueException.java,v 1.2 2001-04-05 19:27:23 mthome Exp $
   **/

/**
 * A RuleParameterException is an exception to be generated when setting
 * a parameter with values illegal for that parameter instance.
 */
public class RuleParameterIllegalValueException extends Exception {
  /**
   * Constructor - Contains parameter type and message
   * @param int parameter_type for type code of parameter (from RuleParameter)
   * @param String message 
   */
  public RuleParameterIllegalValueException
      (int parameter_type, String message) 
  { 
    my_parameter_type = parameter_type; 
    my_message = message; 
  }

  /**
   * Accessor to parameter type code
   * @return int parameter type code
   */
  public int ParameterType() { return my_parameter_type; }

  /**
   * Accessor to text message
   * @return String text message
   */
  public String Message() { return my_message; }

  protected int my_parameter_type;
  protected String my_message;
}

