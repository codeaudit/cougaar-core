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

import java.util.EventObject;

/**
 * CommunityChangeEvent is used to notify interested parties that a change
 * has occurred in a community of interest.  The event contains a reference
 * to the community generating the event.  The event also contains attributes
 * identifying the cheange type and affected entity.  Since event generators
 * may not generate separate events for each change these attributes can only
 * be assumed to reflect the most recent change.
 */
public class CommunityChangeEvent extends EventObject {

  // Defines the type of change
  public static final int ADD_COMMUNITY                = 1;
  public static final int REMOVE_COMMUNITY             = 2;
  public static final int COMMUNITY_ATTRIBUTES_CHANGED = 3;
  public static final int ADD_ENTITY                   = 4;
  public static final int REMOVE_ENTITY                = 5;
  public static final int ENTITY_ATTRIBUTES_CHANGED    = 6;

  protected Community community;
  protected int type;
  protected String whatChanged;

  /**
   *
   * @param community    Changed community
   * @param type         Type of most recent change
   * @param whatChanged  Name of entity associated with most recent change
   */
  public CommunityChangeEvent(Community community, int type, String whatChanged) {
    super(community.getName());
    this.community = community;
    this.type = type;
    this.whatChanged = whatChanged;
  }

  /**
   * Returns a reference to changed community.
   * @return  Reference to changed community
   */
  public Community getCommunity() {
    return community;
  }

  /**
   * Returns name of community generating event.
   * @return Name of changed community
   */
  public String getCommunityName() {
    return community.getName();
  }

  /**
   * Returns a code indicating the type of the most recent change.
   * @return  Change code
   */
  public int getType() {
    return type;
  }

  /**
   * Returns the name of the Entity associated with the most recent change.
   * @return  Entity name.
   */
  public String getWhatChanged() {
    return whatChanged;
  }

  /**
   * Returns a string representation of the change code.
   * @param changeType
   * @return  Change code as a string
   */
  public static String getChangeTypeAsString(int changeType) {
    switch (changeType) {
      case ADD_COMMUNITY: return "ADD_COMMUNITY";
      case REMOVE_COMMUNITY: return "REMOVE_COMMUNITY";
      case COMMUNITY_ATTRIBUTES_CHANGED: return "COMMUNITY_ATTRIBUTES_CHANGED";
      case ADD_ENTITY: return "ADD_ENTITY";
      case REMOVE_ENTITY: return "REMOVE_ENTITY";
      case ENTITY_ATTRIBUTES_CHANGED: return "ENTITY_ATTRIBUTES_CHANGED";
    }
    return "INVALID_VALUE";
  }

  /**
   * Returns a string representation of the change event.
   * @return Event as a string
   */
  public String toString() {
    String communityName = getCommunityName();
    return "CommunityChangeEvent:" +
      " community=" + (communityName == null ? "*" : communityName) +
      " type=" + getChangeTypeAsString(type) +
      " whatChanged=" + whatChanged;
  }
}
