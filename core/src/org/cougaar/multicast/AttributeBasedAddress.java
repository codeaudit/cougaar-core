/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
package org.cougaar.multicast;

import java.io.Serializable;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;

import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.DirectiveImpl;

/**
 * Attribute Based Messaging support class, 
 * used in <code>Blackboard</code> for determining those
 * directive destinations not specified by a 
 * <code>ClusterIdentifier</code>
 */
public class AttributeBasedAddress extends ClusterIdentifier implements Serializable
{
  String communityName;
  String attributeName, attributeValue;
  
  public AttributeBasedAddress(String commName, String attrName, String attrValue) {
    communityName = commName;
    attributeName = attrName;
    attributeValue = attrValue;
  }
  
  public String getAttributeValue() {
    return attributeValue;
  }
  
  public String getAttributeName() {
    return attributeName;
  }

  public String getCommunityName() {
    return communityName;
  }

  public boolean isPersistable(){
    return false;
  }
}
