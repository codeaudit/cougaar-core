/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.policy;

 /** 
   * @author  ALPINE <alpine-software@bbn.com>
   *
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

