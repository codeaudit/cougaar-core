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


final class TimeSliceScheduler extends Scheduler
{
    private static final long DEFAULT_SLICE_DURATION = 1000;
    private int outstandingChildSliceCount;
    TimeSlice[] timeSlices;

    TimeSliceScheduler(ThreadListenerProxy listenerProxy, String name) {
	super(listenerProxy, name);
	outstandingChildSliceCount = 0;
    }


    // The root needs to make slices here
    void setParent(Scheduler parent) {
	super.setParent(parent);
	if (parent == null) {
	    timeSlices = new TimeSlice[maxRunningThreads];
	    for (int i=0; i<maxRunningThreads; i++) {
		timeSlices[i] = new TimeSlice();
	    }
	}
    }


    // Only the root does this
    private TimeSlice findSlice() {
	TimeSlice slice = null;
	for (int i=0; i<maxRunningThreads; i++) {
	    slice = timeSlices[i];
	    if (!slice.in_use) 	return slice;
	}
	return null;
    }




    synchronized void changedMaxRunningThreadCount() {
	runMoreThreads();
    }



    synchronized void wakeup() {
	// Let the children know that a slice may be available.
	Iterator itr = children.iterator();
	while (itr.hasNext()) {
	    Scheduler child = (Scheduler) itr.next();
	    child.wakeup();
	}
	runMoreThreads();
    }


    private void runMoreThreads() {
	// Maybe we can run some pending threads
	while (!pendingThreads.isEmpty()) {
	    TimeSlice slice = getSlice(null);
	    if (slice == null) break;
	    runNextThread(slice);
	}
    }



    // Getting and releasing TimeSlices.  For now, only the root
    // ThreadService does any work here.

    // If we're keeping track of slices we give to children, here's
    // where we get to decide whether or not we're willing to grant
    // another one.
    private boolean grantSliceP(TimeSliceScheduler child) {
	int use_count = runningThreadCount+outstandingChildSliceCount;
	if (use_count < maxRunningThreads) {
	    return true;
	} else {
	    if (DebugThreads)
		System.out.println("No slices available from " +name+
				   " for " +child+
				   "; " +outstandingChildSliceCount+
				   " outstanding");
	    return false;
	}
	
    }

    // If we're keeping track of slices we give to children, here's
    // where we get to note a grant
    private void noteGrant(TimeSlice slice, TimeSliceScheduler child) {
	++outstandingChildSliceCount;
    }

    

    private synchronized TimeSlice getSlice(TimeSliceScheduler child) {
	TimeSlice slice = null;

	if (child != null && !grantSliceP(child)) {
	    slice = null;
	} else if (parent != null) {
	    slice = ((TimeSliceScheduler) parent).getSlice(this);
	    if (child != null && slice != null) noteGrant(slice, child);
	} else {
	    // The root.
	    slice = findSlice();

	    // For now make a new one everytime.  Later cache them.
	    long start = System.currentTimeMillis();
	    long end = start + DEFAULT_SLICE_DURATION;
	    slice.start = start;
	    slice.end = end;
	    slice.in_use = true;
	    
	    noteGrant(slice, child);

	    if (DebugThreads)
		System.out.println(name+
				   " made a slice for " +child+
				   "; " +outstandingChildSliceCount+
				   " now outstanding");
	}
	
	return slice;
    }



    // If we're keeping track of the slices we've given to children,
    // here's where we get told that the given chilc has released the
    // given slice 
    private void noteRelease(TimeSlice slice, TimeSliceScheduler child) {
	--outstandingChildSliceCount;
	if (DebugThreads)
	    System.out.println(name+
			       " released a slice from " +child+
			       "; " +outstandingChildSliceCount+
			       " now outstanding");
    }



    private synchronized void releaseSlice(TimeSlice slice, 
					   TimeSliceScheduler child)
    {
	slice.in_use = false;
	if (child != null) noteRelease(slice, child);
	if (parent != null) {
	    ((TimeSliceScheduler) parent).releaseSlice(slice, this);
	} else {
	    // The top of the tree: notify everyone that slices may be
	    // available.
	    wakeup();
	}
    }




    private void runNextThread(TimeSlice slice) {
	ControllableThread thread =
	    (ControllableThread)pendingThreads.next();
	if (thread != null) {
	    thread.slice(slice);
	    if (DebugThreads)
		System.out.println("Starting " +thread+ " with slice " +slice);
	    thread.start();
	} else {
	    System.err.println("No threads on the queue!");
	}
    }


    private void releaseThreadSlice(ControllableThread thread) {
	TimeSlice slice = thread.slice();
	thread.slice(null);
	releaseSlice(slice, null);
    }



    // Called when a thread is about to end
    void threadReclaimed(ControllableThread thread) {
	super.threadReclaimed(thread);
	synchronized (this) {
	    releaseThreadSlice(thread);
	    // Could re-use slice
	    if (!pendingThreads.isEmpty()) {
		TimeSlice slice  = getSlice(null);
		if (slice != null) runNextThread(slice);
	    }
	}
    }



    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(ControllableThread thread) {
	ControllableThread candidate = null;
	synchronized (this) {
	    boolean expired = thread.slice().isExpired();
	    if (expired) {
		if (DebugThreads) 
		    System.out.println(thread+ "'s slice expired");
		// If our slice expired, just give up control
		// without looking for another thread to yield to.
		threadSuspended(thread);
		releaseThreadSlice(thread);
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

	    threadSuspended(thread);
	    if (DebugThreads) 
		System.out.println(thread+" giving slice to " +candidate);
	    candidate.slice(thread.slice());
		 
	}
	    
	candidate.start();
	return true;
    }



    // Called when a thread is about to suspend.
    synchronized void suspendThread(ControllableThread thread)
    {
	super.suspendThread(thread);
	releaseThreadSlice(thread);
	if (!pendingThreads.isEmpty()) {
	    TimeSlice slice = getSlice(null);
	    if (slice != null) runNextThread(slice);
	}
    }


    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    synchronized boolean maybeResumeThread(ControllableThread thread)
    {
	TimeSlice slice = getSlice(null);
	if (slice != null) {
	    thread.slice(slice);
	    if (DebugThreads)
		System.out.println("Resuming " +thread+ " with slice " +slice);
	    resumeThread(thread);
	    return true;
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	    return false;
	}
    }

 

    synchronized void startOrQueue(ControllableThread thread) {
	// If some external caller has tried to start the thread it
	// should have no slice yet.  If the thread is being started
	// or resumed from the scheduler itself, it will have a slice
	// already.
	TimeSlice slice = thread.slice();
	if (slice == null) {
	    slice = getSlice(null);
	    thread.slice(slice);
	}
	if (slice != null) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
