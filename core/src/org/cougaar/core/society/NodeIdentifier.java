/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

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

