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

import org.cougaar.core.component.Service;
import org.cougaar.core.util.PropertyNameValue;

import java.util.*;

/** A CommunityService is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to community management capabilities.
 */
public interface CommunityService extends Service {


  /**
   * Requests the roster for the named community.
   * @param communityName Name of community
   * @return              Community roster (or null if agent is not authorized
   *                      access)
   */
  CommunityRoster getRoster(String communityName);


  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member.
   * @pram name   Member name
   * @return A collection of community names
   */
  Collection listParentCommunities(String member);


  /**
   * Adds an agent to a community.
   * @param parent   Name of parent community
   * @param member   Name of agent to add
   * @param type     Type of member (Agent or Community)
   * @param roles    Roles provided by member agent
   * @result              True if operation was successful
   */
  boolean addMember(String parent, String member, int type, String[] roles);


  /**
   * Adds an agent to list of agents that are notified of changes to specified
   * community.
   * @param agentName     Name of listener agent
   * @param communityName Community of interest
   * @result              True if operation was successful
   */
  boolean addListener(String agentName, String communityName);


  /**
   * Removes an agent from list of agents that are notified of changes to
   * specified community.
   * @param agentName    Name of listener agent
   * @param communityName Community of interest
   * @result              True if operation was successful
   */
  boolean removeListener(String agentName, String communityName);


  /**
   * Returns a collection of agent names that are currently in the communities
   * listener list.
   * specified community.
   * @param communityName Community of interest
   * @result              Names of listener agents
   */
  Collection getListeners(String communityName);


  /**
   * Removes a member (agent or community) from a community.
   * @param parent        Name of parent community
   * @param member        Name of member to remove
   * @result              True if operation was successful
   */
  boolean removeMember(String parent, String member);


  /**
   * Finds community spokesaents that support a specified role.
   * @param communityName Name of community
   * @param role          Role
   * @return              Collection of Spokesagent names
   */
  Collection findSpokesagentsByRole(String communityName, String role);


  /**
   * Request to get a list of current community spokesagents and roles.
   * @param communityName Name of community
   * @return              SpokesAgentList identifying community spokesagens and
   *                      roles
   */
  SpokesagentList getSpokesagentList(String communityName);


  /**
   * Checks for the existence of a community in Yellow Pages.
   * @param communityName Name of community to look for
   * @return              True if community was found
   */
  boolean communityExists(String communityName);


  /**
   * Creates a new community in Yellow Pages.
   * @param communityName Name of community
   * @param attributes    Community attributes
   * @result              True if operation was successful
   */
  boolean createCommunity(String communityName, PropertyNameValue[] attributes);


  /**
   * Adds a new spokesagent to a community.
   * @param communityName Name of community
   * @param spokesagent   Defines agent, roles and attributes for spokesagent
   * @result              True if operation was successful
   */
  boolean addSpokesagent(String communityName, SpokesagentDescriptor spokesagent);


  /**
   * Removes a spokesagent from a community.
   * @param communityName Name of community
   * @param agent         Identifier for agent to remove
   * @result              True if operation was successful
   */
  boolean removeSpokesagent(String communityName, String agentName);


  /**
   * Lists all communities in Yellow Pages.
   * @result  Collection of community names
   */
  Collection listAllCommunities();


  /**
   * Finds all community members that support a given role.
   * @param communityName Name of community to query
   * @param roleName      Name of role provided by a member
   * @result              Collection of member names
   */
  Collection findMembersByRole(String communityName, String roleName);


  /**
   * Returns a list of roles supported by the specified community.
   * @param communityName Agents parent community
   * @result              Collection of role names
   */
  Collection listCommunityRoles(String communityName);


  /**
   * Returns a list of roles supported by the specified community member.
   * @param communityName Agents parent community
   * @param memberName     Name of community member
   * @result              Collection of members role names
   */
  Collection listAgentRoles(String communityName, String agentName);

}
