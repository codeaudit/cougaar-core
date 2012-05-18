/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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



