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
 * A society-unique reference to a named object in a society.
 **/

public final class UID 
  implements Externalizable, Comparable
{
  private String owner;
  private long id;
  private transient int _hc;

  /** No argument constructor is only for use by serialization! **/
  public UID() {}

  /** @deprecated Use UID(String key, long id); **/
  public UID(String uid) {
    int l = uid.indexOf('/');
    if (l == -1) 
      throw new IllegalArgumentException("String \""+uid+"\" is not a valid UID pattern");
    owner = uid.substring(0,l).intern();
    try {
      id = Long.parseLong(uid.substring(l+1));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("String \""+uid+"\" is not a valid UID pattern");
    }
    _hc = computeHashCode(owner,id);
  }
  
  public UID(String owner, long id) {
    this.owner = owner.intern();
    this.id = id;
    _hc = computeHashCode(this.owner, id);
  }

  public String getOwner() { return owner; }
  public long getId() { return id; }

  public boolean equals(UID other) {
    return (owner == other.owner && id == other.id);
  }

  /*
  public boolean equals(String otheruid) {
    return (uid == otheruid || uid.equals(otheruid));
  }
  */

  public boolean equals(Object other) {
    if (this == other) return true;
    if (other instanceof UID) {
      UID o = (UID) other;
      return (owner == o.owner && id == o.id);
    } else {
      return false;
    }
  }

  public int compareTo(Object other) {
    if (this == other) return 0;

    if (other instanceof UID) {
      UID o = (UID) other;
      String oo = o.owner;
      long oid = o.id;
      /* compare first by string order with owners, then by id */
      int c1 = owner.compareTo(oo);
      if (c1 == 0) {
        if (id == oid) return 0;
        return (id>oid)?1:-1;
      } else {
        return c1;
      }
    } else {
      /* if they aren't both UIDs than the ordering is undefined. */
      return -1;
    }
  }

  public int hashCode() {
    return _hc;
  }
  
  public String toString() {
    /* Ugh! but we really don't want to keep these around! */
    return owner+"/"+id;
  }

  // for user interface, follow beans pattern
  /** @deprecated  Use getOwner and getID **/
  public String getUID() {
    return toString();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(owner);
    out.writeLong(id);
  }
  public void readExternal(ObjectInput in) throws IOException {
    try {
      owner = (String) in.readObject();
      if (owner != null) owner = owner.intern();
      id = in.readLong();
      _hc = computeHashCode(owner, id);
    } catch (Exception e) { throw new IOException(e.toString()); }
  }


  private static final int computeHashCode(String owner, long id) {
    return owner.hashCode()+((int)id);
  }
}
