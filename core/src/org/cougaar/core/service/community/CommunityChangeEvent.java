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

public class CommunityChangeEvent extends EventObject {
  public static final int ADD_COMMUNITY                = 1;
  public static final int REMOVE_COMMUNITY             = 2;
  public static final int COMMUNITY_ATTRIBUTES_CHANGED = 3;
  public static final int ADD_ENTITY                   = 4;
  public static final int REMOVE_ENTITY                = 5;
  public static final int ENTITY_ATTRIBUTES_CHANGED    = 6;

  protected Community community;
  protected int type;
  protected String whatChanged;

  public CommunityChangeEvent(Community community, int type, String whatChanged) {
    super(community.getName());
    this.community = community;
    this.type = type;
    this.whatChanged = whatChanged;
  }


  public Community getCommunity() {
    return community;
  }

  public String getCommunityName() {
    return community.getName();
  }

  public int getType() {
    return type;
  }

  public String getWhatChanged() {
    return whatChanged;
  }

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

  public String toString() {
    String communityName = getCommunityName();
    return "CommunityChangeEvent:" +
      " community=" + (communityName == null ? "*" : communityName) +
      " type=" + getChangeTypeAsString(type) +
      " whatChanged=" + whatChanged;
  }
}
