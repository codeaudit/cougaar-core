/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

/** 
 * OperatingModePolicy specifies constraints on values of Operating
 * modes. It consists of an if clause expressing the conditions under
 * which the policy applies and an array of restrictions on the values
 * of some {@link OperatingMode}s.
 **/

 /* IF clause, then clause
  * If THREATCON > 3 then
  *     (encription > 128) && (encryption < 512).
  */

public class OperatingModePolicy extends PlayBase implements Policy  {

  /**
   * Constructor 
   * @param ifClause the 'if' ConstrainingClause 
   * @param omConstraints an array of constraints to apply to {@link OperatingMode}s
   */
  public OperatingModePolicy (ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    super(ifClause, omConstraints);
  }
  
  /**
   * Returns the originator or creator (authority) of the policy. This
   * is part of the implementation of the Policy interface.
   * @return the name of the authority
   **/
  public String getAuthority() { return ""; }
  
   /**
   * Returns the sender of the policy; the agent that sent it to you.
   * @return the (name of) the sender of the policy.
   */
  public String getSource() { return "";}
}
