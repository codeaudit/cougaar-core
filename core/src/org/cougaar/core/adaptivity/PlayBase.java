package org.cougaar.core.adaptivity;
/** 
 * The Play determines what values to apply to a circumstance.
 */
public class PlayBase {

  private ConstrainingClause ifClause;
  private ConstraintPhrase[] operatingModeConstraints;

  /**
   * Constructor
   * @param ConstraingClause representing the 'if' clause
   * @param ConstraingClause representing the 'then' clause
   **/
  public PlayBase(ConstrainingClause ifClause, ConstraintPhrase[] omConstraints) {
    this.ifClause = ifClause;
    this.operatingModeConstraints = omConstraints;
  }

  /** 
   * A comparison based on sensor data and operating modes 
   * @return the 'if' ConstrainingClause 
   */
  public ConstrainingClause getIfClause() {
    return ifClause;
  }

  /**
   * Knobs with current setting and a range or enumeration of
   * allowable settings for this play in order of desirability. 
   * Should "then" clause be limited to && expressions?
   * @return 'then' ConstrainingClause 
   */ 
  public ConstraintPhrase[] getOperatingModeConstraints() {
    return operatingModeConstraints;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append(ifClause);
    for (int i = 0; i < operatingModeConstraints.length; i++) {
      buf.append(":")
        .append(operatingModeConstraints[i]);
    }
    return buf.toString();
  }
}
