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
import org.cougaar.core.agent.ClusterIdentifier;
import javax.naming.directory.Attributes;

import java.util.*;

/** A CommunityService is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to community management capabilities.
 */
public interface CommunityService extends Service {


  /**
   * Creates a new community in Name Server.
   * @param communityName Name of community
   * @param attributes    Community attributes
   * @result              True if operation was successful
   */
  boolean createCommunity(String communityName, Attributes attributes);


  /**
   * Checks for the existence of a community in Name Server.
   * @param communityName Name of community to look for
   * @return              True if community was found
   */
  boolean communityExists(String communityName);


  /**
   * Lists all communities in Name Server.
   * @result  Collection of community names
   */
  Collection listAllCommunities();


  /**
   * Returns attributes associated with community.
   * @param communityName Name of community
   * @return              Communities attributes
   */
  Attributes getCommunityAttributes(String communityName);


  /**
   * Modifies the attributes associated with a community.
   * @param communityName Name of community
   * @param attributes    Communities attributes
   * @result              True if operation was successful
   */
  boolean setCommunityAttributes(String communityName, Attributes attributes);


  /**
   * Adds an entity to a community.
   * @param communityName        Community name
   * @param entityName           Name of entity to add
   * @param attributes           Attributes to associate with entity
   * @result                     True if operation was successful
   */
  boolean addToCommunity(String communityName, String entityName,
                         Attributes attributes);


  /**
   * Adds an agent to a community.
   * @param communityName        Community name
   * @param agent                Agents ClusterID
   * @param attributes           Attributes to associate with agent
   * @result                     True if operation was successful
   */
  boolean addToCommunity(String communityName, ClusterIdentifier agent,
                         Attributes attributes);


  /**
   * Removes an entity from a community.
   * @param communityName  Community name
   * @param entityName     Name of entity to remove
   * @result               True if operation was successful
   */
  boolean removeFromCommunity(String communityName, String entityName);


  /**
   * Returns attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @result               Attributes associated with entity
   */
  Attributes getEntityAttributes(String communityName, String entityName);


  /**
   * Modifies the attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @param attributes     Attributes to associate with entity
   * @result               True if operation was successful
   */
  boolean setEntityAttributes(String communityName, String entityName,
                              Attributes attributes);


  /**
   * Performs attribute based search of community context.  This search looks
   * for communities with attributes that satisfy criteria specified by filter.
   * Entities within communities are not searched.  This is a general
   * purpose search operation using a JNDI search filter.  Refer to JNDI
   * documentation for filter syntax.
   * @param filter        JNDI search filter
   * @result              Collection of community names that satisfy filter
   */
  Collection search(String filter);


  /**
   * Performs attribute based search of community entities.  This is a general
   * purpose search operation using a JNDI search filter.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @result              Collection of entity names
   */
  Collection search(String communityName, String filter);


  /**
   * Performs attribute based search of community agents.  This is a general
   * purpose search operation using a JNDI search filter.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @result              ClusterIdentifiers for agents satisfying search criteria
   */
  Collection agentSearch(String communityName, String filter);


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
   * Adds an agent to list of agents that are notified of changes to specified
   * community.
   * @param agent         Listener agents ClusterIdentifier
   * @param communityName Community of interest
   * @result              True if operation was successful
   */
  boolean addListener(ClusterIdentifier agent, String communityName);


  /**
   * Removes an agent from list of agents that are notified of changes to
   * specified community.
   * @param agent         Listener agents ClusterIdentifier
   * @param communityName Community of interest
   * @result              True if operation was successful
   */
  boolean removeListener(ClusterIdentifier agent, String communityName);


  /**
   * Returns a collection of ClusterIdentifiers associated with the agents
   * that are have the attribute "ChangeListener".
   * specified community.
   * @param communityName Community of interest
   * @result              ClusterIdentifiers of listener agents
   */
  Collection getListeners(String communityName);


  /**
   * Finds all community entities associated with a given role.  This method
   * is equivalent to using the search method with the filter
   * "(Role=RoleName)".
   * @param communityName Name of community to query
   * @param roleName      Name of role provided
   * @result              Collection of entity names
   */
  Collection searchByRole(String communityName, String roleName);


  /**
   * Performs attribute based search of community agents associated with
   * a given role.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @result              ClusterIdentifiers for agents satisfying search criteria
   */
  Collection agentSearchByRole(String communityName, String roleName);


  /**
   * Returns a collection of all roles supported by the specified community
   * entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @result               Collection of role names
   */
  Collection getEntityRoles(String communityName, String entityName);


  /**
   * Returns a list of all external roles supported by the specified community.
   * @param communityName Community name
   * @result              Collection of role names
   */
  Collection getCommunityRoles(String communityName);


  /**
   * Associates a new role with specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @result               True if operation was successful
   */
  boolean addRole(String communityName, String entityName, String roleName);


  /**
   * Removes a Role from attributes of specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @result               True if operation was successful
   */
  boolean removeRole(String communityName, String entityName, String roleName);

}