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

package org.cougaar.core.persist;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * This class is used to wrap arbitrary objects such that they can be
 * used to uniquely key a hashtable. Equality and hashcode are based
 * entirely on object identity. This is also a WeakReference so that
 * the existence of this key does not force objects to be retained.
 **/
class PersistenceKey extends WeakReference{

  private int hash;
  private int referenceId;

  PersistenceKey(Object o) {
    super(o);
    hash = System.identityHashCode(o); // Get this now before the object disappears
  }

  PersistenceKey(Object o, ReferenceQueue refQ, PersistenceReference id) {
    super(o, refQ);
    hash = System.identityHashCode(o); // Get this now before the object disappears
    referenceId = id.intValue();
  }

  public int hashCode() {
    return hash;
  }

  public boolean equals(Object o2) {
    if (o2 == this) return true; // This is the usual case -- make it first
    if (o2 instanceof PersistenceKey) {
      Object t = this.get();
      Object u = ((PersistenceKey) o2).get();
      if (t == null || u == null) return false;	// Garbage collected -- can't be equal
      return t == u;
    }
    return false;
  }
  public int getReferenceId() {
    return referenceId;
  }
}
