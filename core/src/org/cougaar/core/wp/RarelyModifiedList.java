/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Holds a list that rarely modify.
 * <p>
 * The idea here is that clients wish to iterate over the list
 * at is exists at a specific point in time, and don't want to
 * lock the list to avoid ConcurrentModificationExceptions.  In
 * fact, concurrent modification is intended.  Rather than cloning
 * the list per access, this wrapper assumes that the list is
 * rarely modified, so it clones the list <i>per modification</i>
 * since many more accesses are expected than modifications.
 * <p>
 * Could repackage this into "org.cougaar.util".
 */
public final class RarelyModifiedList 
implements Serializable 
{

  private transient final Object lock = new Object();
  private transient List l;

  public RarelyModifiedList() {
    this.l = Collections.EMPTY_LIST;
  }

  public RarelyModifiedList(List l) {
    synchronized (lock) {
      this.l = 
        (l.isEmpty() ?
         Collections.EMPTY_LIST :
         new ArrayList(l));
    }
  }

  public List getList() {
    synchronized (lock) {
      return l;
    }
  }

  public boolean add(Object o) {
    synchronized (lock) {
      if (l.isEmpty()) {
        l = Collections.singletonList(o);
      } else {
        List newL = new ArrayList(l.size()+1);
        newL.addAll(l);
        newL.add(o);
        l = Collections.unmodifiableList(newL);
      }
    }
    return true;
  }

  public boolean remove(Object o) {
    synchronized (lock) {
      int n = (l == null ? 0 : l.size());
      int i = -1;
      while (true) {
        if (++i >= n) {
          return false;
        }
        if (o == l.get(i)) {
          break;
        }
      }
      if (n == 1) {
        l = Collections.EMPTY_LIST;
      } else {
        List newL = new ArrayList(n-1);
        for (int j = 0; j < i; j++) {
          newL.add(l.get(j));
        }
        for (int j = i+1; j < n; j++) {
          newL.add(l.get(j));
        }
        l = Collections.unmodifiableList(newL);
      }
      return true;
    }
  }

  public String toString() {
    return getList().toString();
  }

  private void writeObject(
      ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    List l = getList();
    stream.writeObject(l);
  }

  private void readObject(
      ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    List l = (List) stream.readObject();
    synchronized (lock) {
      this.l = l;
    }
  }
}
