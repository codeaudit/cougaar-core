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

import java.util.Collection;


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
   * Returns a Collection of ClusterIdentifiers identifying the agents that are
   * currently community members.
   * @return Collection of Agent ClusterIdentifiers
   */
  Collection getMemberAgents();


  /**
   * Returns an collection of CommunityMember objects representing the currenty
   * community membership.
   * @return Collection of CommunityMember objects
   */
  Collection getMembers();


  /**
   * Returns a Collection of community names identifying the communities that are
   * currently a member.
   * @return Collection of community names
   */
  Collection getMemberCommunities();

}
