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

import java.util.EventObject;

public class CommunityChangeEvent extends EventObject {
  public static final int ADD_COMMUNITY    = 1;
  public static final int REMOVE_COMMUNITY = 2;
  public static final int ADD_ENTITY       = 3;
  public static final int REMOVE_ENTITY    = 4;

  protected int type;
  protected String whatChanged;

  public CommunityChangeEvent(String communityName, int type, String whatChanged) {
    super(communityName);
    this.type = type;
    this.whatChanged = whatChanged;
  }

  public String getCommunityName() {
    return (String) getSource();
  }

  public int getType() {
    return type;
  }

  public String getWhatChanged() {
    return whatChanged;
  }

  public String toString() {
    String communityName = getCommunityName();
    return "CommunityChangeEvent("
      + (communityName == null ? "*" : communityName)
      + ", " + getType() + ", " + whatChanged + ")";
  }
}
