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

/**
 * Defines name, type, and supported roles for a community member.
 */

public class CommunityMember implements java.io.Serializable{

  public static final int AGENT     = 0;
  public static final int COMMUNITY = 1;

  private int type;                 // Member type (either AGENT or COMMUNITY)
  private String member;              // Member name
  private Collection roles = new Vector();  // List of roles provided by member

  /**
   * Constructs a CommunityMember object with an empty role collection.
   */
  public CommunityMember(int type, String member) {
    this.type = type;
    this.member = member;
  }

  /**
   * Associates a role with member.
   * @param role Role name
   */
  public void addRoleName(String role) {
    roles.add(role);
  }

  /**
   * Returns the member type (AGENT or COMMUNITY)
   * @return Member type
   */
  public int getType() { return this.type; }

  /**
   * Returns true if this member is an Agent.
   * @return True if an Agent
   */
  public boolean isAgent() { return (type == AGENT); }

  /**
   * Returns member name.
   */
  public String getName() { return this.member; }

  /**
   * Return a collection of role names that this member provides.
   */
  public Collection getRoles() { return this.roles; }

  public boolean equals(Object obj) {
    if (obj instanceof CommunityMember) {
      CommunityMember cm = (CommunityMember)obj;
      return (getType() == cm.getType() && getName().equals(cm.getName()));
    }
    return false;
  }

}