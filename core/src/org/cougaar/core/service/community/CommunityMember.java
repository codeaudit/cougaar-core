/*
 * <copyright>
 *  Copyright 1997-2001 Mobile Intelligence Corp
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
package org.cougaar.core.service.community;

import java.util.*;
import org.cougaar.core.agent.ClusterIdentifier;

/**
 * Defines name, type, and supported roles for a community member.
 */

public class CommunityMember implements java.io.Serializable{

  private String memberName;                // Member name
  private ClusterIdentifier agentId;
  private Collection roles = new Vector();  // List of roles provided by member

  /**
   * Constructs a CommunityMember object for a member community
   * with an empty role collection.
   */
  public CommunityMember(String memberCommunityName) {
    this.memberName = memberCommunityName;
  }

  /**
   * Constructs a CommunityMember object for a member agent
   * with an empty role collection.
   */
  public CommunityMember(ClusterIdentifier memberAgent) {
    this.memberName = memberAgent.toString();
    this.agentId = memberAgent;
  }

  /**
   * Associates a role with member.
   * @param role Role name
   */
  public void addRoleName(String role) {
    roles.add(role);
  }

  /**
   * Returns true if this member is an Agent.
   * @return True if an Agent
   */
  public boolean isAgent() { return (agentId != null); }

  /**
   * Returns the ClusterIdentifier for this member.  Returns null if the
   * member is not an Agent.
   * @return True if an Agent
   */
  public ClusterIdentifier getAgentId() { return agentId; }

  /**
   * Returns member name.
   */
  public String getName() { return this.memberName; }

  /**
   * Return a collection of role names that this member provides.
   */
  public Collection getRoles() { return this.roles; }

  public boolean equals(Object obj) {
    if (obj instanceof CommunityMember) {
      CommunityMember cm = (CommunityMember)obj;
      return (getName().equals(cm.getName()));
    }
    return false;
  }

}