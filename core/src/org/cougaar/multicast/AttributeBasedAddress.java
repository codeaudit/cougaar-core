/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAddressWithAttributes;
import org.cougaar.core.mts.MessageAttributes;

/**
 * Attribute Based Messaging support class, 
 * used in <code>Blackboard</code> for determining those
 * directive destinations not specified by a 
 * <code>MessageAddress</code>
 */
public class AttributeBasedAddress 
  extends MessageAddress
  implements Externalizable
{
  protected transient String myCommunityName;
  protected transient String myAttributeType; 
  protected transient String myAttributeValue;

  public AttributeBasedAddress() {}

  private AttributeBasedAddress(String commName, String attrType, String attrValue) {
    if (commName == null) {
      myCommunityName = "";
    } else {
      myCommunityName = commName;
    }
    myAttributeType = attrType;
    myAttributeValue = attrValue;
  }

  public String getCommunityName() {
    return myCommunityName;
  }

  public String toAddress() {
    return "ABA";
  }
  public String toString() {
    return "#<ABA '"+myCommunityName+"' "+
      myAttributeType+"="+myAttributeValue+">";
  }

  /**
   * @deprecated Use getAttributeType instead.
   **/
  public String getAttributeName() {
    return getAttributeType();
  }

  public String getAttributeType() {
    return myAttributeType;
  }

  public String getAttributeValue() {
    return myAttributeValue;
  }
  

  public boolean isPersistable(){
    return false;
  }

 
  // override MessageAddress
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(myCommunityName);
    out.writeObject(myAttributeType);
    out.writeObject(myAttributeValue);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    myCommunityName = (String) in.readObject();
    myAttributeType = (String) in.readObject();
    myAttributeValue = (String) in.readObject();
  }

  /*
    // totally bogus - do correctly some time
  protected Object readResolve() {
    return new AttributeBasedAddress(myCommunityName, myAttributeType, myAttributeValue);
  }
  */

  // 
  // factories
  //
    
  public static AttributeBasedAddress getAttributeBasedAddress(String commName, String attrType, String attrValue) {
    return new AttributeBasedAddress(commName, attrType, attrValue);
  }

  public static MessageAddress getAttributeBasedAddress(String commName, String attrType, String attrValue, MessageAttributes mas) {
    MessageAddress prime = getAttributeBasedAddress(commName, attrType, attrValue);
    if (mas == null) { 
      return prime;
    } else {
      return MessageAddressWithAttributes.getMessageAddressWithAttributes(prime, mas);
    }
  }


}



