/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.util.UID;

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

public class OperatingModePolicy implements Policy, Publishable  {

  private UID uid = null;
  private String policyName = "";
  private String authority;
  private PolicyKernel policy;

  /**
   * Constructor 
   * @param ifClause the 'if' ConstrainingClause 
   * @param omConstraints an array of constraints to apply to {@link OperatingMode}s
   */
  public OperatingModePolicy (PolicyKernel pk) {
    policy = pk;
  }
  
  public OperatingModePolicy (ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    this(new PolicyKernel(ifClause, omConstraints));
  }
  
  public OperatingModePolicy (String policyName,
			      ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints) {
    this(ifClause, omConstraints);
    this.policyName = policyName;
  }

  public OperatingModePolicy (String policyName,
			      ConstrainingClause ifClause, 
			      ConstraintPhrase[] omConstraints,
			      String authority) {
    this(policyName, ifClause, omConstraints);
    this.authority = authority;
  }

  /**
   * Returns the originator or creator (authority) of the policy. This
   * is part of the implementation of the Policy interface.
   * @return the name of the authority
   **/
  public String getAuthority() { return authority; }
  
  public void setAuthority(String authority) {
    if (this.authority != null) throw new RuntimeException("Attempt to change Policy Authority");
    this.authority = authority;
  }

  public String getName() {
    return policyName;
  }

  public void setName(String name) {
    if (policyName != null) throw new RuntimeException("Attempt to change Policy Name");
    policyName = name;
  }

  // UniqueObject interface
  public UID getUID() {
    return uid;
  }

  /**
   * Set the UID (unique identifier) of this UniqueObject. Used only
   * during initialization.
   * @param uid the UID to be given to this
   **/
  public void setUID(UID uid) {
    if (this.uid != null) throw new RuntimeException("Attempt to change UID: " + uid);
    this.uid = uid;
  }

  public PolicyKernel getPolicyKernel() {
    return policy;
  }

  protected void setPolicyKernel(PolicyKernel pk) {
    policy = pk;
  }


  /* convenience methods */
  public ConstrainingClause getIfClause() {
    return policy.getIfClause();
  }

  public ConstraintPhrase[] getOperatingModeConstraints() {
    return policy.getOperatingModeConstraints();
  }
  
  public String toString() {
    StringBuffer sb = new StringBuffer(getName());
    sb.append(" ");
    sb.append(policy.getIfClause().toString());
    ConstraintPhrase[] cp = policy.getOperatingModeConstraints();
    for (int i=0; i < cp.length; i++) {
      sb.append(": ");
      sb.append(cp[i].toString());
    }
    return sb.toString();
  }

  public boolean isPersistable() {
    return true;
  }
}
