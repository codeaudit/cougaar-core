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
 * 
 * @see org.cougaar.core.cluster.ClusterIdentifier
 */

/**
 * An address for a Message sender or receiver.
 **/

public class MessageAddress implements Externalizable {
  protected transient byte[] addressBytes;
  protected transient int _hc = 0;
  protected transient String _as = null;

  // don't allow subclasses access to default constructor
  public MessageAddress() {}

  public MessageAddress( String address) {
    this.addressBytes = address.getBytes();
    _as = address.intern();
    _hc = _as.hashCode();
  }

  /** @return The address of a society member.  This is Society-centric and
   * may not be human readable or parsable.
   **/
  public final String getAddress() {
    return _as;
  }

  public boolean equals(MessageAddress ma ){
    return (_as== ma._as);
  }

  public boolean equals(Object o ){
    if (this == o) return true;
    // use == since the strings are interned.
    if (o instanceof MessageAddress) {
      MessageAddress oma = (MessageAddress) o;
      return (_as== oma._as);
    } else {
      return false;
    }
  }

  public String toString() {
    return _as;
  }

  private String getString() {
    return _as;
  }

  /** @return the object address part of a URL describing the entity on
   * the COUGAAR society's pseudo-web.  e.g. the URL of an entity could be 
   * contstructed with something like protocol+"://"+host+":"+port+"/"+getAddress()+"/";
   **/
  public String toAddress() {
    return _as;
  }

  public final int hashCode() { 
    return _hc;
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    int l = addressBytes.length;
    out.writeByte(l);
    out.write(addressBytes,0,l);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    int l = in.readByte();
    addressBytes=new byte[l];
    in.readFully(addressBytes,0,l);
    _as = new String(addressBytes).intern();
    _hc = _as.hashCode();
  }

  public static final MessageAddress SOCIETY = new MulticastMessageAddress("SOCIETY");
  public static final MessageAddress COMMUNITY = new MulticastMessageAddress("COMMUNITY");
  public static final MessageAddress LOCAL = new MulticastMessageAddress("LOCAL");

  
}
