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

package org.cougaar.core.thread;

import java.util.ArrayList;
import java.util.Comparator;

import org.cougaar.util.log.Logger;
import org.cougaar.util.UnaryPredicate;

/**
 * A simple queue, built on array list, that uses a Comparator to
 * determine which elements is next (the smallest, according to the
 * Comparator).  Note that this is not a Collection.  Also note that
 * the methods are not synchronized.  It's the caller's reponsibility
 * to handle synchronization.  Queues of this kind are used by @link
 * Schedulers to hold @link Schedulables that are not able to run
 * immediately.
 */
public class DynamicSortedQueue
{
    private Comparator comparator;
    private ArrayList store;
    
    // Only used for the Scheduler's iterateOverQueuedThreads method,
    // so that it can read the elements without locking the thread
    // service or risking damage to the real queue.  DO NOT USE THIS
    // FOR ANY OTHER PURPOSE.
    DynamicSortedQueue(DynamicSortedQueue queue)
    {
	this.store = new ArrayList(queue.store);
    }

    public DynamicSortedQueue(Comparator comparator) {
	store = new ArrayList();
	this.comparator = comparator;
    }

    // This should ONLY be called by the ThreadStatusService.  It's
    // unsafe otherwise.
    int processEach(ThreadStatusService.Body body, 
		    String schedulerName,
		    Logger logger) 
    {
	int count = 0;
	Object thing;
	for (int i = 0, n = store.size(); i < n; i++) {
	    try {
		thing = store.get(i);
	    } catch (Exception ex) {
		// This is an expected condition to end the loop
		if (logger.isDebugEnabled())
		    logger.debug("queue size decreased during list operation");
		break;
	    }
	    if (thing != null) {
		try {
		    SchedulableObject sched = (SchedulableObject) thing;
		    long startTime = sched.getTimestamp();
		    body.run(schedulerName, sched);
		    count++;
		} catch (Throwable t) {
		    logger.error("ThreadStatusService error in body", t);
		}
	    }
	}
	return count;
    }

    public ArrayList filter(UnaryPredicate predicate) {
	ArrayList result = new ArrayList();
	for (int i = 0, n = store.size(); i < n; i++) {
	    Object candidate = store.get(i);
	    if (!predicate.execute(candidate)) {
		result.add(candidate);
		store.remove(i);
                i--;
                n--;
	    }
	}
	return result;
    }

    public String toString() {
	return "<DQ[" +store.size()+ "] " +store.toString()+ ">";
    }


    public boolean contains(Object x) {
	return store.contains(x);
    }

    public void setComparator(Comparator comparator) {
	this.comparator = comparator;
    }

    public int size() {
	return store.size();
    }

    public boolean add(Object x) {
	if (store.contains(x)) return false;
	store.add(x);
	return true;
    }


    public void remove(Object x) {
	store.remove(x);
    }
	    

    public boolean isEmpty() {
	return store.isEmpty();
    }


    public Object next() {
	Object min = null;
	for (int i = 0, n = store.size(); i < n; i++) {
	    Object candidate = store.get(i);
	    if (min == null) {
		min = candidate;
	    } else {
		int comp = comparator.compare(min, candidate);
		if (comp > 0) min = candidate;
	    }
	}
	if (min != null) store.remove(min);
	return min;
    }
	    
}

