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

  /** @deprecated Use toUID(String) or UID(String, long) **/
  public UID(String uid) {
    // duplicate some of "toUID(String)"
    int l = uid.indexOf('/');
    if (l == -1)
      throw new IllegalArgumentException(
          "String \""+uid+"\" is not a valid UID pattern");
    owner = uid.substring(0,l).intern();
    try {
      id = Long.parseLong(uid.substring(l+1));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          "String \""+uid+"\" is not a valid UID pattern");
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
  
  /**
   * Convert a UID into a String.
   * <p>
   * One should <u>not</u> assume a readable format for
   * this String, or that it has a guaranteed format!
   *
   * @see #toUID(String)
   */
  public String toString() {
    /* Ugh! but we really don't want to keep these around! */
    return owner+"/"+id;
  }

  /**
   * Parse the given String into a UID object useful for doing searches and comparisons.
   * <p>
   * The String's format should match <tt>UID.toString()</tt>.
   * <p>
   * There is no guarantee that this UID will be valid or
   * has been assigned to an Object in the society.
   *
   * @see #toString()
   */
  public static UID toUID(String uid) {
    String tOwner;
    long tId;
    try {
      int l = uid.indexOf('/');
      tOwner = uid.substring(0,l);
      tId = Long.parseLong(uid.substring(l+1));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          "String \""+uid+"\" is not a valid UID pattern");
    }
    return new UID(tOwner, tId);
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
