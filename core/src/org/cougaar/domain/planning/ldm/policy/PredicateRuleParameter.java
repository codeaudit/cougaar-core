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
import org.cougaar.util.UnaryPredicate;

/** 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: PredicateRuleParameter.java,v 1.1 2001-02-15 19:45:20 tomlinso Exp $
 **/

/**
 * An PredicateRuleParameter is a RuleParameter with a UnaryPredicate
 * as its value. The inRange method is implemented to apply the
 * predicate to the test object.
 **/
public class PredicateRuleParameter implements RuleParameter, java.io.Serializable {

  /**
   * Constructor sets the predicate
   **/
  public PredicateRuleParameter(String param_name, UnaryPredicate aPredicate) { 
    name = param_name;
    thePredicate = aPredicate;
  }

  /**
   * Parameter type is PREDICATE
   */
  public int ParameterType() {
    return RuleParameter.PREDICATE_PARAMETER;
  }

  /**
   * Get parameter value (Integer)
   * @returns Object parameter value (Integer). Note : could be null.
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


  public String getName() 
  {
    return name;
  }

  public Object clone() {
    return new PredicateRuleParameter(name, thePredicate);
  }

  protected String name;
  protected UnaryPredicate thePredicate;
}
