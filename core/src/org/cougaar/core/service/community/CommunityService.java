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
import org.cougaar.core.mts.MessageAddress;
import javax.naming.directory.Attributes;

import java.util.*;

/** A CommunityService is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to community management capabilities.
 */
public interface CommunityService extends Service {
  String COMMUNITIES_CONTEXT_NAME = "Communities";


  /**
   * Creates a new community in Name Server.
   * @param communityName Name of community
   * @param attributes    Community attributes
   * @return              True if operation was successful
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
   * @return  Collection of community names
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
   * @return              True if operation was successful
   */
  boolean setCommunityAttributes(String communityName, Attributes attributes);


  /**
   * Adds an entity to a community.
   * @param communityName        Community name
   * @param entity               Entity to add
   * @param entityName           Name of entity
   * @param attributes           Attributes to associate with entity
   * @return                     True if operation was successful
   */
  boolean addToCommunity(String communityName, Object entity,
                         String entityName, Attributes attributes);


  /**
   * Removes an entity from a community.
   * @param communityName  Community name
   * @param entityName     Name of entity to remove
   * @return               True if operation was successful
   */
  boolean removeFromCommunity(String communityName, String entityName);


  /**
   * Returns a collection of entity names associated with the specified
   * community.
   * @param communityName  Entities parent community
   * @return               Collection of entity names
   */
  Collection listEntities(String communityName);


  /**
   * Returns attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @return               Attributes associated with entity
   */
  Attributes getEntityAttributes(String communityName, String entityName);


  /**
   * Modifies the attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @param attributes     Attributes to associate with entity
   * @return               True if operation was successful
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
   * @return              Collection of community names that satisfy filter
   */
  Collection search(String filter);


  /**
   * Performs attribute based search of community entities.  This is a general
   * purpose search operation using a JNDI search filter.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @return              Collection of entity objects
   */
  Collection search(String communityName, String filter);


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
   * @param name   Member name
   * @return A collection of community names
   */
  Collection listParentCommunities(String member);


  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member and satisfy a given set of attributes.
   * @param name   Member name
   * @param filter Search filter defining community attributes
   * @return A collection of community names
   */
  Collection listParentCommunities(String member, String filter);


  /**
   * Adds a listener to list of addresses that are notified of changes to
   * specified community.
   * @param addr          Listeners address
   * @param communityName Community of interest
   * @return              True if operation was successful
   */
  boolean addListener(MessageAddress addr, String communityName);


  /**
   * Removes a listener from list of addresses that are notified of changes to
   * specified community.
   * @param addr          Listeners address
   * @param communityName Community of interest
   * @return              True if operation was successful
   */
  boolean removeListener(MessageAddress addr, String communityName);


  /**
   * Returns a collection of MessageAddresses associated with the entities
   * that have the attribute "ChangeListener".
   * specified community.
   * @param communityName Community of interest
   * @return              Collection of listener MessageAddresses
   */
  Collection getListeners(String communityName);


  /**
   * Finds all community entities associated with a given role.  This method
   * is equivalent to using the search method with the filter
   * "(Role=RoleName)".
   * @param communityName Name of community to query
   * @param roleName      Name of role provided
   * @return              Collection of entity objects
   */
  Collection searchByRole(String communityName, String roleName);


  /**
   * Returns a collection of all roles supported by the specified community
   * entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @return               Collection of role names
   */
  Collection getEntityRoles(String communityName, String entityName);


  /**
   * Returns a list of all external roles supported by the specified community.
   * @param communityName Community name
   * @return              Collection of role names
   */
  Collection getCommunityRoles(String communityName);


  /**
   * Associates a new role with specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @return               True if operation was successful
   */
  boolean addRole(String communityName, String entityName, String roleName);


  /**
   * Removes a Role from attributes of specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @return               True if operation was successful
   */
  boolean removeRole(String communityName, String entityName, String roleName);

  void addListener(CommunityChangeListener l);

  void removeListener(CommunityChangeListener l);
}
