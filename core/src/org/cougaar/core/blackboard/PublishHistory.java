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
package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Date;

/**
 * Records the recent publication history of alp objects to help
 * explain apparently anomalous publish events.
 **/
public class PublishHistory {
    /**
     * Item records the current publication history of an object as a
     * stack dump (Throwable) for each of add, change, and remove. It
     * also has the time of the last publish event.
     **/
    private static class Item implements Comparable {
        public Throwable add, change, remove;
        public long lastTime;
        public void recordAdd() {
            add = new Throwable("add@" + new Date(lastTime));
        }
        public void recordChange() {
            change = new Throwable("change@" + new Date(lastTime));
        }
        public void recordRemove() {
            remove = new Throwable("remove@" + new Date(lastTime));
        }
        public int compareTo(Object o) {
            Item other = (Item) o;
            if (other == this) return 0;
            long diff = lastTime - other.lastTime;
            if (diff > 0L) return 1;
            if (diff < 0L) return -1;
            return this.hashCode() - other.hashCode();
        }
        public void dumpStacks() {
            if (add != null)
                add.printStackTrace(System.out);
            else
                System.out.println("No add recorded");
            if (change != null)
                change.printStackTrace(System.out);
            else
                System.out.println("No change recorded");
            if (remove != null)
                remove.printStackTrace(System.out);
            else
                System.out.println("No remove recorded");
        }
    }

    /**
     * WeakReference extension having a Object that is a key to the
     * map Map. The values in that Map are Ref objects referring to an
     * Item. When the Ref in the map is the only remaining reference
     * to the Item, the map entry is removed. The key Object is kept
     * in the Ref because it is much faster to remove the entry by
     * using its key than by using its value.
     **/
    private static class Ref extends WeakReference {
        public Object object;
        public Ref(Object object, Item item, ReferenceQueue refQ) {
            super(item, refQ);
            this.object = object;
        }
    }

    /**
     * A set of Items sorted by time. The least recently referenced
     * Items should be at the head of the Set so they can be quickly
     * removed as time elapses.
     **/
    private static SortedSet items = new TreeSet();
    private static long nextCheckItems = System.currentTimeMillis() + 60000L;

    /**
     * Update the last reference time of an Item. The Item must be
     * removed from the sorted set prior to modifying its time because
     * the time is the basis for the sorting of the Set.
     **/
    private static synchronized void updateItem(Item item) {
        items.remove(item);
        item.lastTime = System.currentTimeMillis();
        items.add(item);
    }

    /** Used to form the headset of items older than a certain time **/
    private static Item deleteItem = new Item();

    /**
     * Remove Items older than 1 minute.
     **/
    private static synchronized void checkItems() {
        long now = System.currentTimeMillis();
        if (now < nextCheckItems) return;
        nextCheckItems = now + 60000L;
        deleteItem.lastTime = now - 60000L;
        int removeCount = 0;
        for (Iterator i = items.headSet(deleteItem).iterator(); i.hasNext(); ) {
            i.remove();
            removeCount++;
        }
//          if (removeCount > 0) System.out.println("PublishHistory removed " + removeCount + " items");
    }

    /**
     * Map from published object to Ref to Item;
     **/
    private Map map = new HashMap();

    private ReferenceQueue refQ = new ReferenceQueue();

    /**
     * Get the Item corresponding to a published Object. A new item is
     * created if necessary. In all cases, the lastTime of the Item is
     * updated to now.
     **/
    private Item getItem(Object o) {
        Ref ref;
        Item item;
        checkItems();
        do {
            while ((ref = (Ref) refQ.poll()) != null) {
                map.remove(ref.object);
            }
            ref = (Ref) map.get(o);
            if (ref == null) {
                item = new Item();
                ref = new Ref(o, item, refQ);
                map.put(o, ref);
            } else {
                item = (Item) ref.get();
            }
        } while (item == null);
        updateItem(item);
        return item;
    }

    /**
     * Record a stack trace in the add slot of the item corresponding
     * to a Object.
     **/
    public void publishAdd(Object o) {
        getItem(o).recordAdd();
    }

    /**
     * Record a stack trace in the change slot of the item
     * corresponding to a Object.
     **/
    public void publishChange(Object o) {
        getItem(o).recordChange();
    }

    /**
     * Record a stack trace in the remove slot of the item
     * corresponding to a Object.
     **/
    public void publishRemove(Object o) {
        getItem(o).recordRemove();
    }
    public void dumpStacks(Object o) {
        Item item = getItem(o);
        item.dumpStacks();
    }
}
