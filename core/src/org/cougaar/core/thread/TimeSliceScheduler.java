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


final class TimeSliceScheduler extends Scheduler
{

    TimeSliceScheduler(ThreadListenerProxy listenerProxy, String name) {
	super(listenerProxy, name);
    }

    private void runMoreThreads() {
	// Maybe we can run some pending threads
	while (!pendingThreads.isEmpty()) {
	    TimeSlice slice = getSlice();
	    if (slice == null) break;
	    runNextThread(slice);
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


    // Parent offers us a slice
    public synchronized boolean offerSlice(TimeSlice slice) {
	if (!pendingThreads.isEmpty()) {
	    runNextThread(slice);
	    return true;
	} else {
	    return false;
	}
    }



    private void releaseThreadSlice(ControllableThread thread) {
	TimeSlice slice = thread.slice();
	thread.slice(null);
	releaseSlice(slice);
    }



    private synchronized void handoffSlice(ControllableThread thread) {
	boolean expired = thread.slice().isExpired();
	boolean queue_empty = pendingThreads.isEmpty();
	if (expired) {
	    if (DebugThreads) 
		System.out.println(thread+ "'s slice expired");
	    releaseThreadSlice(thread);
	    // Now look for some other thread to run, in some other
	    // slice.
	    if (!queue_empty) {
		TimeSlice slice  = getSlice();
		if (slice != null) runNextThread(slice);
	    }
	} else if (!queue_empty) {
	    // Reuse the slice
	    TimeSlice slice = thread.slice();
	    thread.slice(null);
	    runNextThread(slice);
	} else {
	    // No other threads to run
	    releaseThreadSlice(thread);
	}
    }
    


    // Called when a thread is about to end
    void threadReclaimed(ControllableThread thread) {
	super.threadReclaimed(thread);
	handoffSlice(thread);
    }

    // Called when a thread is about to suspend.
    synchronized void suspendThread(ControllableThread thread)
    {
	super.suspendThread(thread);
	handoffSlice(thread);
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




    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    synchronized boolean maybeResumeThread(ControllableThread thread)
    {
	TimeSlice slice = getSlice();
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
	    slice = getSlice();
	    thread.slice(slice);
	}
	if (slice != null) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }


    synchronized void changedMaxRunningThreadCount() {
	runMoreThreads();
    }

}
