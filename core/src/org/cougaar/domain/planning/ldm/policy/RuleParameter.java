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

/**
 * A RuleParameter is generic object containing a parameter value
 *
 * Values may be one of several types:
 *    INTEGER - Integer value type (within given bounds)
 *    DOUBLE - Double value type (within given bounds)
 *    STRING - String value type
 *    LONG - Long value type (within given bounds)
 *    ENUMERATION - Enumeration value type (String from given list)
 *    BOOLEAN - Boolean value type
 *    CLASS - Java class value type (implementing given interface)
 *    KEY - Set of String values (with default) indexed off a key
 *    RANGE - Set of values (String or RuleParameter) (with default) indexed 
 *        from a list of integer ranges
 *
 */
public interface RuleParameter extends Cloneable {

  /**
   * Define list of constant parameter types:
   */
  public final static int INTEGER_PARAMETER = 1;
  public final static int DOUBLE_PARAMETER = 2;
  public final static int STRING_PARAMETER = 3;
  public final static int ENUMERATION_PARAMETER = 4;
  public final static int BOOLEAN_PARAMETER = 5;
  public final static int CLASS_PARAMETER = 6;
  public final static int KEY_PARAMETER = 7;
  public final static int RANGE_PARAMETER = 8;
  public final static int LONG_PARAMETER = 9;
  public final static int PREDICATE_PARAMETER = 10;

  /**
   * Type of given parameter
   * @return int type of given parameter
   */
  public int ParameterType();

  /**
   * Get parameter object value for parameter
   * @return Object with given parameter value. Note : could be null.
   */
  public Object getValue();

  /**
   * Set parameter object value 
   * @param Object new_value - the new value to be set
   * @throws RuleParameterIllegalValueException if value set is illegal for 
   * given parameter
   */
  public void setValue(Object new_value) 
       throws RuleParameterIllegalValueException;

  /**
   * Test the value to see if it is valid.
   * @param Object test_object - the value to be tested
   * @return true if the test_object is within the allowable range, false
   * otherwise.
   **/
  public boolean inRange(Object test_object);

  /**
   * @return the name of the parameter
   **/
  public String getName();

  public Object clone();
}


