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
import java.util.Set;

import javax.naming.directory.Attributes;

import org.cougaar.core.service.community.CommunityChangeEvent;
import org.cougaar.core.service.community.CommunityChangeListener;

/**
 * Defines the attributes and child entities for a community.  This is the
 * primary class used by the community service infrastructure to describe the
 * details of a single community.  Instances of this class are published
 * to the blackboards of community members and other interested agents.
 */
public interface Community extends Entity {

  // Search qualifiers
  public static final int AGENTS_ONLY = 0;
  public static final int COMMUNITIES_ONLY = 1;
  public static final int ALL_ENTITIES = 2;

  /**
   * Returns a collection containing all entities associated with this
   * community.
   * @return  Collection of Entity objects
   */
  public Collection getEntities();

  /**
   * Returns named Entity or null if it doesn't exist.
   * @param  Name of requested entity
   * @return named entity
   */
  public Entity getEntity(String name);

  /**
   * Returns true if community contains entity.
   * @param  Name of requested entity
   * @return true if community contains entity
   */
  public boolean hasEntity(String name);

  /**
   * Adds an Entity to the community.
   * @param entity  Entity to add to community
   */
  public void addEntity(Entity entity);

  /**
   * Removes an Entity from the community.
   * @param entityName  Name of Entity to remove from community
   */
  public void removeEntity(String entityName);

  /**
   * Performs search of community and returns collection of matching Entity
   * objects.
   * @param filter    JNDI style search filter
   * @param qualifier Search qualifier (e.g., AGENTS_ONLY, COMMUNITIES_ONLY, or
   *                  ALL_ENTITIES)
   * @return Set of Entity objects satisfying search filter
   */
  public Set search(String filter,
                    int    qualifier);

  public String qualifierToString(int qualifier);

}