package org.cougaar.core.adaptivity;

/** 
 * A phrase used to express a boolean comparison between a string
 * standing in for sensor data or an operating mode and a Object
 * holding the value and a set of valid values
 */
public class ConstraintPhrase {
  String proxyName;
  ConstraintOperator operator;
  OMSMValueList allowedValues;
  
  /**
   * Constructor 
   * @param String name of the input source, e.g., sensor name
   * @param ConstraintOperator
   * @param an array of OMSMRange descriptions list allowed ranges.
   */
  public ConstraintPhrase(String name, ConstraintOperator op, OMSMValueList rp) {
    proxyName = name;
    operator = op;
    allowedValues = rp;
  }
  
  /** 
   * @return The name of the sensor or operating mode 
   */
  public String getProxyName() {
    return proxyName;
  }
  
  /** 
   * Get the effective value of the allowed values. This is always the
   * the min of the first range.
   * @return the value as a Comparable (String, Integer, Double, etc.)
   **/
  public Comparable getValue() {
    return allowedValues.getEffectiveValue();
  }

  /**
   * Get the ranges of allowed values.
   * @return all allowed ranges as imposed by this constraint
   **/
  public OMSMValueList getAllowedValues() {
    return allowedValues;
  }
  
  /** 
   * The relationship between the sensor or operating mode and the
   * value.
   * @return ConstraintOperator */
  public ConstraintOperator getOperator() {
    return operator;
  }

  public String toString() {
    return proxyName + " " + operator + " " + allowedValues;
  }
}
