package org.cougaar.core.adaptivity;
interface Policy {

  /**
   * The originator of the policy 
   */
  String getAuthority();

  /** 
   * The immediate source of this policy. That is, one who
   * forwarded the policy to this agent.
   */
  String getSource();



}
