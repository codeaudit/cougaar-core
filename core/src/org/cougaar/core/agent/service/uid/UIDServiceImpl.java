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

package org.cougaar.core.agent.service.uid;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/** Essential services for serving UniqueObjects to
 * parts of a cluster.
 **/
final class UIDServiceImpl implements UIDService {
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
