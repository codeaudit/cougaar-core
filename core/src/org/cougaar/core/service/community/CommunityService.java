/*
 * <copyright>
 *  Copyright 1997-2003 Mobile Intelligence Corp
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

import java.util.Collection;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/** A CommunityService is an API which may be supplied by a
 * ServiceProvider registered in a ServiceBroker that provides
 * access to community management capabilities.
 */
public interface CommunityService extends Service {

  // Entity types
  public static final int AGENT = 0;
  public static final int COMMUNITY = 1;

  /**
   * Request to create a community.  If the specified community does not
   * exist it will be created and the caller will become the community
   * manager.  It the community already exists a descriptor is obtained
   * from its community manager and returned in the response.
   * @param communityName    Name of community to create
   * @param attrs            Attributes to associate with new community
   * @param crl              Listener to receive response
   */
  void createCommunity(String                    communityName,
                       Attributes                attrs,
                       CommunityResponseListener crl);

  /**
   * Request to join a named community.  If the specified community does not
   * exist it may be created in which case the caller becomes the community
   * manager.  It the community doesn't exist and the caller has set the
   * "createIfNotFound flag to false the join request will be queued until the
   * community is found.
   * @param communityName    Community to join
   * @param entityName       New member name
   * @param entityType       Type of member entity to create (AGENT or COMMUNITY)
   * @param entityAttrs      Attributes for new member
   * @param createIfNotFound Create community if it doesn't exist, otherwise
   *                         wait
   * @param communityAttrs   Attributes for created community (used if
   *                         createIfNotFound set to true, otherwise ignored)
   * @param crl              Listener to receive response
   */
  void joinCommunity(String                    communityName,
                     String                    entityName,
                     int                       entityType,
                     Attributes                entityAttrs,
                     boolean                   createIfNotFound,
                     Attributes                newCommunityAttrs,
                     CommunityResponseListener crl);

  /**
   * Request to leave named community.
   * @param communityName  Community to leave
   * @param entityName     Entity to remove from community
   * @param crl            Listener to receive response
   */
  void leaveCommunity(String                    communityName,
                      String                    entityName,
                      CommunityResponseListener crl);

  /**
   * Request to add named community to local cache and register for update
   * notifications.
   * @param communityName  Community of interest, if null listener will receive
   *                       notification of changes in all communities
   * @param timeout        Time (in milliseconds) to wait for operation to
   *                       complete before returning response (-1 = wait forever)
   * @param crl            Listener to receive response
   */
  void getCommunity(String                    communityName,
                    long                      timeout,
                    CommunityResponseListener crl);

  /**
   * Request to modify an Entity's attributes.
   * @param communityName    Name of community
   * @param entityName       Name of affected Entity or null if modifying
   *                         community attributes
   * @param mods             Attribute modifications
   * @param crl              Listener to receive response
   */
  public void modifyAttributes(String                    communityName,
                               String                    entityName,
                               ModificationItem[]        mods,
                               CommunityResponseListener crl);
  /**
   * Initiates a community search operation. The results are provided via a
   * call back to a specified CommunitySearchListener.
   * @param communityName   Name of community to search
   * @param searchFilter    JNDI compliant search filter
   * @param recursiveSearch True for recursive search into nested communities
   *                        [false = search top community only]
   * @param resultQualifier Type of entities to return in result [ALL_ENTITIES,
   *                        AGENTS_ONLY, or COMMUNITIES_ONLY]
   * @param crl             Callback object to receive search results
   */
  void searchCommunity(String                    communityName,
                       String                    searchFilter,
                       boolean                   recursiveSearch,
                       int                       resultQualifier,
                       CommunityResponseListener crl);

  /**
   * Returns a list of all communities of which caller is a member.
   * @param allLevels Set to false if the list should contain only those
   *                  communities in which the caller is explicitly
   *                  referenced.  If true the list will also include those
   *                  communities in which the caller is implicitly a member
   *                  as a result of community nesting.
   * @param crl       Callback object to receive search results
   */
  void getParentCommunities(boolean                   allLevels,
                            CommunityResponseListener crl);

  /**
   * Add listener for CommunityChangeEvents.
   * @param l  Listener
   */
  void addListener(CommunityChangeListener l);

  /**
   * Remove listener for CommunityChangeEvents.
   * @param l  Listener
   */
  void removeListener(CommunityChangeListener l);

  /////////////////////////////////////////////////////////////////////////////
  // D E P R E C A T E D    M E T H O D s
  /////////////////////////////////////////////////////////////////////////////

    String COMMUNITIES_CONTEXT_NAME = "Communities";

  /**
   * Creates a new community in Name Server.
   * @param communityName Name of community
   * @param attributes    Community attributes
   * @return              True if operation was successful
   * @deprecated          Use joinCommunity with createIfNotFound parameter
   */
  boolean createCommunity(String communityName, Attributes attributes);


  /**
   * Checks for the existence of a community in Name Server.
   * @param communityName Name of community to look for
   * @return              True if community was found
   * @deprecated          Use getCommunity method with a timeout of 0ms.  If
   *                      specified community does not exist a null Community
   *                      will be returned in response.
   */
  boolean communityExists(String communityName);


  /**
   * Lists all communities in Name Server.
   * @return  Collection of community names
   * @deprecated
   */
  Collection listAllCommunities();


  /**
   * Returns attributes associated with community.
   * @param communityName Name of community
   * @return              Communities attributes
   * @deprecated
   */
  Attributes getCommunityAttributes(String communityName);


  /**
   * Sets the attributes associated with a community.
   * @param communityName Name of community
   * @param attributes    Communities attributes
   * @return              True if operation was successful
   * @deprecated
   */
  boolean setCommunityAttributes(String communityName, Attributes attributes);


  /**
   * Modifies the attributes associated with a community.
   * @param communityName Name of community
   * @param mods          Attribute modifications to be performed
   * @return              True if operation was successful
   * @deprecated
   */
  boolean modifyCommunityAttributes(String communityName, ModificationItem[] mods);


  /**
   * Adds an entity to a community.
   * @param communityName        Community name
   * @param entity               Entity to add
   * @param entityName           Name of entity
   * @param attributes           Attributes to associate with entity
   * @return                     True if operation was successful
   * @deprecated
   */
  boolean addToCommunity(String communityName, Object entity,
                         String entityName, Attributes attributes);


  /**
   * Removes an entity from a community.
   * @param communityName  Community name
   * @param entityName     Name of entity to remove
   * @return               True if operation was successful
   * @deprecated
   */
  boolean removeFromCommunity(String communityName, String entityName);


  /**
   * Returns a collection of entity names associated with the specified
   * community.
   * @param communityName  Entities parent community
   * @return               Collection of entity names
   * @deprecated
   */
  Collection listEntities(String communityName);


  /**
   * Returns attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @return               Attributes associated with entity
   * @deprecated
   */
  Attributes getEntityAttributes(String communityName, String entityName);


  /**
   * Sets the attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @param attributes     Attributes to associate with entity
   * @return               True if operation was successful
   * @deprecated
   */
  boolean setEntityAttributes(String communityName, String entityName,
                              Attributes attributes);


  /**
   * Modifies the attributes associated with specified community entity.
   * @param communityName  Entities parent community
   * @param entityName     Name of community entity
   * @param mods           Attribute modifications to be performed
   * @return               True if operation was successful
   * @deprecated
   */
  boolean modifyEntityAttributes(String communityName, String entityName,
                                 ModificationItem[] mods);


  /**
   * Performs attribute based search of community context.  This search looks
   * for communities with attributes that satisfy criteria specified by filter.
   * Entities within communities are not searched.  This is a general
   * purpose search operation using a JNDI search filter.  Refer to JNDI
   * documentation for filter syntax.
   * @param filter        JNDI search filter
   * @return              Collection of community names that satisfy filter
   * @deprecated
   */
  Collection search(String filter);


  /**
   * Performs attribute based search of community entities.  This is a general
   * purpose search operation using a JNDI search filter.  This method is
   * non-blocking.  An empty Collection will be returned if the local cache is
   * empty.  Updated search results can be obtained by using the addListener
   * method to receive change notifications.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @return              Collection of MessageAddress objects
   * @deprecated
   */
  Collection search(String communityName, String filter);


  /**
   * Performs attribute based search of community entities.  This is a general
   * purpose search operation using a JNDI search filter.  This method may
   * be invoked in a blocking mode in which case the method may block for
   * an extended period of time if the specified community is not found in
   * the local cache.
   * @param communityName Name of community to search
   * @param filter        JNDI search filter
   * @param blockingMode  Set to true if blocking mode is required
   * @return              Collection of MessageAddress objects
   * @deprecated
   */
  Collection search(String communityName, String filter, boolean blockingMode);


  /**
   * Requests the roster for the named community.
   * @param communityName Name of community
   * @return              Community roster (or null if agent is not authorized
   *                      access)
   * @deprecated
   */
  CommunityRoster getRoster(String communityName);


  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member.
   * @param name   Member name
   * @return A collection of community names
   * @deprecated
   */
  Collection listParentCommunities(String member);


  /**
   * Requests a collection of community names identifying the communities that
   * contain the specified member and satisfy a given set of attributes.
   * @param name   Member name
   * @param filter Search filter defining community attributes
   * @return A collection of community names
   * @deprecated
   */
  Collection listParentCommunities(String member, String filter);


  /**
   * Adds a listener to list of addresses that are notified of changes to
   * specified community.
   * @param addr          Listeners address
   * @param communityName Community of interest
   * @return              True if operation was successful
   * @deprecated
   */
  boolean addListener(MessageAddress addr, String communityName);


  /**
   * Removes a listener from list of addresses that are notified of changes to
   * specified community.
   * @param addr          Listeners address
   * @param communityName Community of interest
   * @return              True if operation was successful
   * @deprecated
   */
  boolean removeListener(MessageAddress addr, String communityName);


  /**
   * Returns a collection of MessageAddresses associated with the entities
   * that have the attribute "ChangeListener".
   * specified community.
   * @param communityName Community of interest
   * @return              Collection of listener MessageAddresses
   * @deprecated
   */
  Collection getListeners(String communityName);


  /**
   * Finds all community entities associated with a given role.  This method
   * is equivalent to using the search method with the filter
   * "(Role=RoleName)".
   * @param communityName Name of community to query
   * @param roleName      Name of role provided
   * @return              Collection of entity objects
   * @deprecated
   */
  Collection searchByRole(String communityName, String roleName);


  /**
   * Returns a collection of all roles supported by the specified community
   * entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @return               Collection of role names
   * @deprecated
   */
  Collection getEntityRoles(String communityName, String entityName);


  /**
   * Returns a list of all external roles supported by the specified community.
   * @param communityName Community name
   * @return              Collection of role names
   * @deprecated
   */
  Collection getCommunityRoles(String communityName);


  /**
   * Associates a new role with specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @return               True if operation was successful
   * @deprecated
   */
  boolean addRole(String communityName, String entityName, String roleName);


  /**
   * Removes a Role from attributes of specified community entity.
   * @param communityName  Parent community
   * @param entityName     Name of community entity
   * @param roleName       Name of role to associate with entity
   * @return               True if operation was successful
   * @deprecated
   */
  boolean removeRole(String communityName, String entityName, String roleName);


}
