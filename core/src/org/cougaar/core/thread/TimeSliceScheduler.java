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

package org.cougaar.core.thread;

import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.PropertyParser;

import java.util.Comparator;
import java.util.Iterator;


final class TimeSliceScheduler extends AbstractScheduler
{
    private static final long DEFAULT_SLICE_DURATION = 1000;
    private int outstandingChildSliceCount;

    TimeSliceScheduler(ThreadListenerProxy listenerProxy) {
	super(listenerProxy);
	outstandingChildSliceCount = 0;
    }


    synchronized void changedMaxRunningThreadCount() {
	runMoreThreads();
    }



    synchronized void wakeup() {
	// Let the children know that a slice may be available.
	Iterator itr = children.iterator();
	while (itr.hasNext()) {
	    AbstractScheduler child = (AbstractScheduler) itr.next();
	    child.wakeup();
	}
	runMoreThreads();
    }


    private void runMoreThreads() {
	// Maybe we can run some pending threads
	while (!pendingThreads.isEmpty()) {
	    TimeSlice slice = getSlice();
	    if (slice == null) break;
	    runNextThread(slice);
	}
    }



    // Getting and releasing TimeSlices.  For now, only the root
    // ThreadService does any work here.

    private synchronized TimeSlice getSlice() {
	if (parent != null)  return ((TimeSliceScheduler) parent).getSlice();

	// If we get here, we're supplying a slice to a child
	int use_count = runningThreadCount+outstandingChildSliceCount;
	if (use_count < maxRunningThreads) {
	    ++outstandingChildSliceCount;
	    long start = System.currentTimeMillis();
	    long end = start + DEFAULT_SLICE_DURATION;
	    System.out.println("Made a slice, " 
			       +outstandingChildSliceCount+
			       " outstanding");
	    return new TimeSlice(start, end);
	}

	System.out.println("No slices available, " 
			   +outstandingChildSliceCount+
			   " outstanding");
	return null;
    }

    private synchronized void releaseSlice(TimeSlice slice) {
	if (parent != null) {
	    ((TimeSliceScheduler) parent).releaseSlice(slice);
	    return;
	}

	// A child has released a slice
	--outstandingChildSliceCount;
	System.out.println("Released a slice, " 
			   +outstandingChildSliceCount+
			   " outstanding");
	wakeup();
    }




    private void runNextThread(TimeSlice slice) {
	ControllableThread thread =
	    (ControllableThread)pendingThreads.next();
	if (thread != null) {
	    thread.slice(slice);
	    thread.start();
	} else {
	    System.out.println("No threads on the queue!");
	}
    }


    private void releaseSliceToParent(ControllableThread thread) {
	--runningThreadCount; 
	thread.slice().in_use = false;
	((TimeSliceScheduler) parent).releaseSlice(thread.slice());
    }



    // Called when a thread is about to end
    void threadEnded(ControllableThread thread) {
	synchronized (this) {
	    releaseSliceToParent(thread);
	    // Could re-use slice
	    if (!pendingThreads.isEmpty()) {
		TimeSlice slice  = getSlice();
		if (slice != null) runNextThread(slice);
	    }
	}
	super.threadEnded(thread);
    }



    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(ControllableThread thread) {
	ControllableThread candidate = null;
	synchronized (this) {
	    boolean expired = thread.slice().isExpired();
	    if (expired) {
		// If our slice expired, just give up control
		// without looking for another thread to yield to.
		releaseSliceToParent(thread);
		return true;
	    }

	    // Slice is still good.  See if a queued thread is
	    // available to yield to.  If so, give it our slice.

	    if ( pendingThreads.isEmpty()) {
		// No pending threads
		return false;
	    }
		
	    candidate = (ControllableThread) pendingThreads.next(thread);
	    if (candidate == thread) {
		// No better-or-equal thread on the queue.
		return false;
	    }

	    --runningThreadCount;
	    candidate.slice(thread.slice());
		 
	}
	    
	candidate.start();
	return true;
    }



    // Called when a thread is about to suspend.
    synchronized void suspendThread(ControllableThread thread)
    {
	releaseSliceToParent(thread);
	if (!pendingThreads.isEmpty()) {
	    TimeSlice slice = getSlice();
	    if (slice != null) runNextThread(slice);
	}
    }


    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    synchronized boolean maybeResumeThread(ControllableThread thread)
    {
	TimeSlice slice = getSlice();
	if (slice != null) {
	    thread.slice(slice);
	    ++runningThreadCount;
	    return true;
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	    return false;
	}
    }

 

    synchronized void startOrQueue(ControllableThread thread) {
	TimeSlice slice = getSlice();
	if (slice != null) {
	    thread.slice(slice);
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
