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

package org.cougaar.core.agent.service.uid;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/** Essential services for serving UniqueObjects to
 * parts of a cluster.
 **/

public final class UIDServiceImpl implements UIDService {
  private MessageAddress cid;
  private String prefix;
  private long count = System.currentTimeMillis();

  public UIDServiceImpl(MessageAddress cid) {
    this.cid = cid;
    prefix = cid.getAddress();
  }

  /** MessageAddress of the proxy server.
   *  This might go away if we ever really separated proxy 
   * servers from clusters.
   **/
  public MessageAddress getMessageAddress() {
    return cid;
  }

  // long getNextUniqueID();
  private synchronized long nextID() {
    return ++count;
  }

  /** get the next Unique ID for the Proxiable object registry
   * at this server.
   * It is better for Factories to use the registerUniqueObject method.
   **/
  public UID nextUID() {
    return new UID(prefix, nextID());
  }

  /** assign a new UID to a unique object.
   **/
  public UID registerUniqueObject(UniqueObject o) {
    UID uid = nextUID();
    o.setUID(uid);
    return uid;
  }
    
  /** called by persistence to get a snapshot of the state. **/
  public synchronized PersistenceState getPersistenceState() {
    return new UIDServerPersistenceState(count);
  }

  /** called during persistence rehydration to reset the state **/
  public synchronized void setPersistenceState(PersistenceState state) {
    if (state instanceof UIDServerPersistenceState) {
      long persistedCount = ((UIDServerPersistenceState)state).count;
      if (persistedCount > count) count = persistedCount;
    } else {
      throw new IllegalArgumentException(state.toString());
    }
  }

  /** private implementation of PersistenceState for saving the
   * UID counter.
   **/
  private static class UIDServerPersistenceState implements PersistenceState {
    public long count;
    public UIDServerPersistenceState(long count) {
      this.count = count;
    }
  }
}
