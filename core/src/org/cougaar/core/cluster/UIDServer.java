/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.persist.PersistenceState;
import org.cougaar.core.cluster.persist.StatePersistable;

/** Essential services for serving UniqueObjects to
 * parts of a cluster.
 **/

public final class UIDServer implements UIDService {
  private ClusterContext cc;
  private ClusterIdentifier cid;
  private String prefix;
  private long count = System.currentTimeMillis();

  public UIDServer(ClusterContext context) {
    cc = context;
    cid = context.getClusterIdentifier();
    prefix = cid.getAddress();
  }

  /** ClusterIdentifier of the proxy server.
   *  This might go away if we ever really separated proxy 
   * servers from clusters.
   **/
  public ClusterIdentifier getClusterIdentifier() {
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
