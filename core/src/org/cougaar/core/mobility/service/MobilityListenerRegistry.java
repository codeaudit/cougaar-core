/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility.service;

import java.util.*;

import org.cougaar.core.mobility.MobilityListener;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;

/**
 * Registry for node-internal mobility listeners.
 */
class MobilityListenerRegistry {

  private final Map map;
  private int counter;

  public MobilityListenerRegistry() {
    this(13);
  }

  public MobilityListenerRegistry(int size) {
    map = new HashMap(size);
  }

  /**
   * Add the given listener, get a listener identifier.
   */
  public Object add(
      MessageAddress agentId, MobilityListener listener) {
    synchronized (map) {
      MultiMobilityListener mml = 
        (MultiMobilityListener) map.get(agentId);
      if (mml == null) {
        mml = new MultiMobilityListener(agentId);
        map.put(agentId, mml);
      }
      Object id = nextCounter();
      mml.add(id, listener);
      return id;
    }
  }

  /**
   * Remove the listener with the specified identifier.
   */
  public boolean remove(
      MessageAddress agentId, Object id) {
    boolean ret = false;
    synchronized (map) {
      MultiMobilityListener mml = 
        (MultiMobilityListener) map.get(agentId);
      if (mml != null) {
        ret = mml.remove(id);
        if (mml.isEmpty()) {
          map.remove(agentId);
        }
      }
    }
    return ret;
  }

  /**
   * Remove all listeners for the given agent.
   */
  public void removeAll(MessageAddress agentId) {
    synchronized (map) {
      map.remove(agentId);
    }
  }

  public void onDispatch(
      MessageAddress agentId, Ticket ticket) {
    List l;
    synchronized (map) {
      MultiMobilityListener mml = 
        (MultiMobilityListener) map.get(agentId);
      if (mml == null) {
        return;
      }
      l = mml.getListeners();
    }
    // FIXME what if the listener was removed?
    for (int i = 0, n = l.size(); i < n; i++) {
      MobilityListener ml = (MobilityListener) l.get(i);
      ml.onDispatch(ticket);
    }
  }

  public void onArrival(
      MessageAddress agentId, Ticket ticket) {
    List l;
    synchronized (map) {
      MultiMobilityListener mml = 
        (MultiMobilityListener) map.get(agentId);
      if (mml == null) {
        return;
      }
      l = mml.getListeners();
    }
    for (int i = 0, n = l.size(); i < n; i++) {
      MobilityListener ml = (MobilityListener) l.get(i);
      ml.onArrival(ticket);
    }
  }

  public void onFailure(
      MessageAddress agentId, Ticket ticket, Throwable throwable) {
    List l;
    synchronized (map) {
      MultiMobilityListener mml = 
        (MultiMobilityListener) map.get(agentId);
      if (mml == null) {
        return;
      }
      l = mml.getListeners();
    }
    for (int i = 0, n = l.size(); i < n; i++) {
      MobilityListener ml = (MobilityListener) l.get(i);
      ml.onFailure(ticket, throwable);
    }
  }

  private Object nextCounter() {
    synchronized (map) {
      return new Integer(++counter);
    }
  }

  /**
   * Container for all MobilityListeners that are associated with 
   * the same agent-id.
   */
  private static class MultiMobilityListener {

    private final MessageAddress agentId;
    private final Map listeners = new HashMap(5);

    public MultiMobilityListener(MessageAddress agentId) {
      this.agentId = agentId;
      if (agentId == null) {
        throw new IllegalArgumentException("null agent id");
      }
    }

    public boolean isEmpty() {
      return listeners.isEmpty();
    }

    public void add(Object id, MobilityListener l) {
      if (!(agentId.equals(l.getAddress()))) {
        throw new IllegalArgumentException(
            "Incorrect agent id "+l.getAddress()+" != "+agentId);
      }
      // assert (!(listeners.contains(id)))
      listeners.put(id, l);
    }

    public boolean remove(Object id) {
      Object o = listeners.remove(id);
      return (o == null);
    }

    public void clear() {
      listeners.clear();
    }

    public List getListeners() {
      if (listeners.isEmpty()) {
        return Collections.EMPTY_LIST;
      }
      return new ArrayList(listeners.values());
    }

    public int hashCode() {
      return agentId.hashCode();
    }

    public String toString() {
      return "multi-listener for "+agentId;
    }
  }
}
