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

import org.cougaar.core.mts.MessageAddress;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A <code>MessageAddress</code> that indicates the type of Message a recipient
 * will have registered an interest in. This "role" must be 
 * expanded to a list of actual recipients for delivery.
 *
 * @see ABMTransportLP
 */
public final class MessageType extends MessageAddress {

  /** only for infrastructure **/
  public MessageType() {}

  public MessageType( String address ) {
    super(address);
    cacheMessageType(address, this);
  }

  public String toString() {
    return getAddress();
  }

  /**
   * Equivalent to toString()
   * @return a <code>String</code> value
   */
  public String toAddress() {
    return getAddress();
  }
  
  /**
   * Equivalent to toString()
   * @return a <code>String</code> value
   */
  public String cleanToString() {
    return getAddress();
  }

  // override MessageAddress
  public void writeExternal(ObjectOutput out) throws IOException {
    int l = addressBytes.length;
    out.writeByte(l);
    out.write(addressBytes,0,l);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    int l = in.readByte();
    addressBytes=new byte[l];
    in.readFully(addressBytes,0,l);
  }

  protected Object readResolve() {
    return getMessageType(new String(addressBytes));
  }

  // Helper methods to ensure efficiencey follow

  private static java.util.HashMap cache = new java.util.HashMap(89);
  public static MessageType getMessageType(String as) {
    synchronized (cache) {
      MessageType a = (MessageType) cache.get(as);
      if (a != null) 
        return a;
      else
        return new MessageType(as.intern());    // calls cacheMessageType
    }
  }

  public static void cacheMessageType(String as, MessageType a) {
    synchronized (cache) {
      cache.put(as, a);
    }
  }
}
