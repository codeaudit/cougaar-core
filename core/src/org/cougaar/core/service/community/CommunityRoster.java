/*
 * <copyright>
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


/** CommunityRoster Interface
  * A CommunityRoster identifies the agents that are currently the member
  * of a specified community.
  **/

public interface CommunityRoster {


  /**
   * This method identifies whether the community associated with this roster
   * currently exists.
   * @return True if the community exists
   **/
  boolean communityExists();


  /**
   * The getCommunityName method returns the name of the community that
   * this roster represents.
   * @return Returns the community name
   **/
  String getCommunityName();


  /**
   * Returns an array of Agent names identifying the agent that are
   * currently a community member.
   * @return Array of Agent names
   */
  String[] getMemberAgents();


  /**
   * Returns an array of CommunityMember objects representing the currenty
   * community membership.
   * @return Array of CommunityMember objects
   */
  CommunityMember[] getMembers();


  /**
   * Returns an array of community names identifying the communities that are
   * currently a member.
   * @return Array of community names
   */
  String[] getMemberCommunities();

}
