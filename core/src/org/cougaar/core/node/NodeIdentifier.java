/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

import java.io.*;

/**
 * Identifier for Nodes in the Society.  The identifier
 * is intended to be unique and non-changing for the lifetime
 * of the Node as it participates within a Society.
 */
public final class NodeIdentifier extends MessageAddress {

  /** only for infrastructure **/
  public NodeIdentifier() {}

  public NodeIdentifier(String address) {
    super(address);
    cacheNodeIdentifier(address, this);
  }

  public NodeIdentifier(MessageAttributes attrs) {
      super(attrs);
  }

  public NodeIdentifier(MessageAttributes attrs, String address) {
    super(attrs, address);
    cacheNodeIdentifier(address, this);
  }

  public String toString() {
    return getAddress();
  }

  public String toAddress() {
    return getAddress();
  }
  
  public String cleanToString() {
    return getAddress();
  }


  // override MessageAddress
  public void writeExternal(ObjectOutput out) throws IOException {
    int l = addressBytes.length;
    out.writeByte(l);
    out.write(addressBytes,0,l);
  }

  public void readExternal(ObjectInput in) 
      throws ClassNotFoundException, IOException {
    int l = in.readByte();
    addressBytes=new byte[l];
    in.readFully(addressBytes,0,l);
  }

  protected Object readResolve() {
    return getNodeIdentifier(new String(addressBytes));
  }

  private static java.util.HashMap cache = new java.util.HashMap(89);
  public static NodeIdentifier getNodeIdentifier(String as) {
    synchronized (cache) {
      NodeIdentifier a = (NodeIdentifier) cache.get(as);
      if (a != null) 
        return a;
      else
        return new NodeIdentifier(as.intern());    // calls cacheNodeIdentifier
    }
  }

  public static void cacheNodeIdentifier(String as, NodeIdentifier a) {
    synchronized (cache) {
      cache.put(as, a);
    }
  }

}

