package org.cougaar.core.adaptivity;
/** 
 * The Play determines what values to apply to a circumstance.
 */
public class Play extends PlayBase {
  /**
   * Constructor
   * @param ConstraingClause representing the 'if' clause
   * @param ConstraingClause representing the 'then' clause
   **/
  public Play (ConstrainingClause ifClause, ConstraintPhrase[] playConstraints) {
    super(ifClause, playConstraints);
  }
}



