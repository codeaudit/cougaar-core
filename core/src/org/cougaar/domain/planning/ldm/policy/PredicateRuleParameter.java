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

import org.cougaar.domain.planning.ldm.policy.RuleParameter;
import org.cougaar.domain.planning.ldm.policy.RuleParameterIllegalValueException;

import org.cougaar.util.UnaryPredicate;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: PredicateRuleParameter.java,v 1.4 2001-08-22 20:14:18 mthome Exp $
 **/

/**
 * An PredicateRuleParameter is a RuleParameter with a UnaryPredicate
 * as its value. The inRange method is implemented to apply the
 * predicate to the test object.
 **/
public class PredicateRuleParameter implements RuleParameter, java.io.Serializable {
  protected String my_name;
  protected UnaryPredicate thePredicate;

  /**
   * Constructor sets the predicate
   **/
  public PredicateRuleParameter(String param_name, UnaryPredicate aPredicate) { 
    my_name = param_name;
    thePredicate = aPredicate;
  }

  public PredicateRuleParameter() {
  }

  /**
   * Parameter type is PREDICATE
   */
  public int ParameterType() {
    return RuleParameter.PREDICATE_PARAMETER;
  }

  public String getName() {
    return my_name;
  }

  public void  setName(String name) {
    my_name = name;
  }

  /**
   * Get parameter value (UnaryPredicate)
   * @returns Object parameter value (UnaryPredicate). Note : could be null.
   */
  public Object getValue() {
    return thePredicate;
  }

  /**
   * Convenience accessor not requiring casting the result
   **/
  public UnaryPredicate getPredicate() {
    return thePredicate;
  }

  /**
   * Set parameter value
   * @param Object new_value : must be Integer
   * @throws RuleParameterIllegalValueException
   */
  public void setValue(Object newPredicate) 
       throws RuleParameterIllegalValueException
  {
    if (!(newPredicate instanceof UnaryPredicate)) {
      throw new RuleParameterIllegalValueException
	(RuleParameter.PREDICATE_PARAMETER, 
	 "Object must be a UnaryPredicate");
    }
    thePredicate = (UnaryPredicate) newPredicate;
  }

  /**
   * 
   * @param Object test_value : Any object
   * @return true if test_value is acceptable to the predicate
   */
  public boolean inRange(Object test_value) {
    if (thePredicate == null) return false;
    return thePredicate.execute(test_value);
  }

  public String toString() 
  {
    return "#<PREDICATE_PARAMETER : " + thePredicate.toString();
  }

  public Object clone() {
    return new PredicateRuleParameter(my_name, thePredicate);
  }

}
