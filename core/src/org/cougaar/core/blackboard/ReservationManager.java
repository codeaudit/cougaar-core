/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.blackboard;

import java.util.Iterator;
import java.util.LinkedList;

import org.cougaar.util.log.Logger;

/**
 * Persistence reservations indicate that a persistence instance
 * wishes to take a snapshot of its agent. The reservations are held
 * in a queue (FIFO). When a persistence instance reaches the head of
 * the queue it has exclusive use of the persistence mechanism. The
 * reservation will only be held for a certain interval and if not
 * exercised or re-confirmed within that interval, it is cancelled.
 * During this interval, the agent should be getting itself into a
 * well-defined state so the persistence snapshot will be valid.
 *
 * If at any time after reaching the head of the queue (and trying to
 * reach a well-defined state), an agent discovers that its
 * reservation has been cancelled, it should abandon its attempt to
 * reach a well-defined state, continue execution, and try again
 * later.
 *
 * If a ReservationManager is created with a timeout of 0, the manager
 * is effectively disabled. This means that all requests and commits
 * are satisfied unconditionally, and waitFor and release return
 * immediately and do nothing. Also no storage is allocated.
 **/

public class ReservationManager {
  private LinkedList queue = null;
  private long timeout;
  private boolean committed;

  private class Item {
    private Object obj;
    private long expires;
    public Item(Object p, long now) {
      obj = p;
      updateTimestamp(now);
    }

    public boolean hasExpired(long now) {
      return expires <= now;
    }

    public void updateTimestamp(long now) {
      expires = now + timeout;
    }

    public String toString() {
      return obj.toString();
    }
  }

  public ReservationManager(long timeout) {
    this.timeout = timeout;
    if (timeout > 0L) {
      queue = new LinkedList();
    }
  }

  public synchronized boolean request(Object p) {
    if (queue == null) return true;
    long now = System.currentTimeMillis();
    Item item = findOrCreateItem(p, now);
    if (!committed) removeExpiredItems(now);
    boolean result = item == queue.getFirst();
    return result;
  }

  private Item findOrCreateItem(Object p, long now) {
    Item item = findItem(p);
    if (item == null) {
      item = new Item(p, now);
      queue.add(item);
    } else {
      item.updateTimestamp(now);
    }
    return item;
  }

  public synchronized void waitFor(Object p, Logger logger) {
    if (queue == null) return;
    while (true) {
      long now = System.currentTimeMillis();
      Item item = findOrCreateItem(p, now);
      if (!committed) removeExpiredItems(now);
      if (item == queue.getFirst()) return;
      try {
        Item firstItem = (Item) queue.getFirst();
        long delay = firstItem.expires - now;
        if (logger != null && logger.isInfoEnabled()) {
          logger.info("waitFor " + delay + " for " + firstItem);
        }
        if (delay <= 0) {
          wait();               // Must be committed, wait for release
        } else {
          wait(delay);          // Uncommitted, wait for timeout or release.
        }
        if (logger != null && logger.isInfoEnabled()) {
          logger.info("waitFor wait finished");
        }
      } catch (InterruptedException ie) {
      }
    }
  }

  public synchronized boolean commit(Object p) {
    if (queue == null) return true;
    if (request(p)) {
      committed = true;
      return true;
    }
    return false;
  }

  public synchronized void release(Object p) {
    if (queue == null) return;
    Item item = findItem(p);
    if (item != null) {
      queue.remove(item);
      committed = false;
      notifyAll();
    }
  }

  private void removeExpiredItems(long now) {
    for (Iterator i = queue.iterator(); i.hasNext(); ) {
      Item item = (Item) i.next();
      if (item.hasExpired(now)) {
        i.remove();
      }
    }
  }

  private Item findItem(Object p) {
    for (Iterator i = queue.iterator(); i.hasNext(); ) {
      Item item = (Item) i.next();
      if (item.obj == p) {
        return item;
      }
    }
    return null;
  }
}
