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

package org.cougaar.core.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A society-unique reference to a named object in a society.
 **/

public final class UID 
  implements Externalizable, Comparable
{
  private String owner;
  private long id;

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
  }
  
  public UID(String owner, long id) {
    this.owner = owner.intern();
    this.id = id;
  }

  public String getOwner() { return owner; }
  public long getId() { return id; }

  public boolean equals(UID other) {
    return (other != null && owner == other.owner && id == other.id);
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
    return owner.hashCode()+((int)id);
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
      if (l == -1) {
        throw new IllegalArgumentException("String \""+uid+"\" is not a valid UID pattern");
      }
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
    } catch (Exception e) { throw new IOException(e.toString()); }
  }
}
