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

import org.cougaar.util.UnaryPredicate;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: PredicateRuleParameter.java,v 1.3 2001-04-05 19:27:23 mthome Exp $
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
