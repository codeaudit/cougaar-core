package org.cougaar.core.adaptivity;

/** 
 * OperatingModePolicy applies constraints on values of Operating modes.
 */ 

 /* IF clause, then clause
  * If THREATCON > 3 then
  *     (encription > 128) && (encryption < 512).
  */

public class OperatingModePolicy extends PlayBase implements Policy  {

  /**
   * Constructor 
   * @param the 'if' ConstrainingClause 
   * @param the 'then' ConstrainingClause
   */
  public OperatingModePolicy (ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    super(ifClause, omConstraints);
  }
  
  /**
   * Returns the originator or creator of the policy.
   * @return String 
   */
  public String getAuthority() { return ""; }
  
   /**
   * Returns the sender of the policy; the agent that sent it to you.
   * @return String 
   */
  public String getSource() { return "";}
}
