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

import org.cougaar.core.blackboard.Publishable;


/** CommunityRequest Interface
  * The CommunityRequest provides a mechanism for components
  * to interact with the community management infrastructure.  A
  * CommunityRequest is a request by an agent to the community
  * management infrastructure to perform a specific task (such
  * as joining a community).
  **/

public interface CommunityRequest extends Publishable {

  /**
   * The getVerb method returns the verb of the CommunityRequest.
   * For example, in the CommunityRequest "join 1ad_community...", the
   * Verb is the object represented by "join".
   * @return Returns the verb string for this CommunityRequest.
   **/

  String getVerb();


  /**
   * The getAgentName method identifies the name of the agent
   * that is the target of this CommunityRequest.
   * @return Name of the agent that is target of this CommunityRequest
   **/

  String getAgentName();


  /**
   * The getTargetCommunityName method identifies the name of community
   * that is the target of the CommunityRequest.
   * For example, in the CommunityRequest "join 1ad_community...", the
   * target community is the object represented by "1ad_community".
   * @return Name of community that is target of this CommunityDirective
   **/

  String getTargetCommunityName();


  /**
   * The getSourceCommunityName method identifies the name of the community
   * of which the target agent is currently a member.
   * For example, in the CommunityRequest "reassign agent1 from communityA
   * to communityB", the source community is the object represented by
   * "communityA" and the target community is the object represented by
   * "communityB".
   * @return Name of community of which the target agent is currently a member
   **/

  String getSourceCommunityName();


  /**
   * The getRoleName method retrieves the name of a role provided by an agent.
   * @return Name of role provided by agent.
   **/

  String getRole();


   /**
   * Returns the response object associated with this CommunityRequest.
   * @return CommunityResponse object associated with this request.
   */
  CommunityResponse getCommunityResponse();


  /**
   * The setVerb method sets the verb of the CommunityRequest.
   * For example, in the CommunityRequest "join 1ad_community...", the
   * Verb is the object represented by "join".
   * @param verb - The verb of the CommunityRequest.
   * <PRE>
   * Supported Verbs:
   *   GET_ROSTER               - Gets membership list associated with community
   *                              specified by TargetCommunity
   *   GET_ROSTER_WITH_UPDATES  - Gets membership list associated with community
   *                              specified by TargetCommunity.  Changes in
   *                              membership are automatically applied to the
   *                              generated roster.
   *   LIST_PARENT_COMMUNITIES  - Gets list of communities of which the
   *                              specified agent is a member
   *   JOIN_COMMUNITY           - Request to join TargetCommunity
   *   LEAVE_COMMUNITY          - Request to leave TargetCommunity
   *   REASSIGN                 - Reassigns the specified agent from the
   *                              SourceCommunity to TargetCommunity
   *   FIND_AGENTS_WITH_ROLE    - Finds agents in community with specified role
   *   GET_ROLES                - Gets list of roles provided by agent
   *   LIST_ALL_COMMUNITIES     - Gets list of all communities curently bound
   *                              in Yellow Pages
   * </PRE>
   **/

  void setVerb(String verb);


  /**
   * The setAgentName method identifies the name of the agent
   * that is the target of this CommunityRequest.
   * @param Name of the agent that is target of this CommunityRequest
   **/

  void setAgentName(String agentName);


  /**
   * The setTargetCommunityName method identifies the name of community
   * that is the target of the CommunityRequest.
   * For example, in the CommunityRequest "join 1ad_community...", the
   * target community is the object represented by "1ad_community".
   * @return Name of community that is target of this CommunityDirective
   **/

  void setTargetCommunityName(String communityName);


  /**
   * The setSourceCommunityName method identifies the name of the community
   * of which the target agent is currently a member.
   * For example, in the CommunityRequest "reassign agent1 from communityA
   * to communityB", the source community is the object represented by
   * "communityA" and the target community is the object represented by
   * "communityB".
   * @return Name of community of which the target agent is currently a member
   **/

  void setSourceCommunityName(String communityName);


  /**
   * The setRoleName method identifies the name of a role provided by an agent.
   * @param roleName  Name of role provided by agent
   **/

  void setRole(String roleName);
}