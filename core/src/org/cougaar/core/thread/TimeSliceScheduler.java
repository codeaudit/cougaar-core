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
	while (pendingThreadCount() > 0) {
	    TimeSlice slice = getSlice();
	    if (slice == null) break;
	    runNextThread(slice);
	}
    }



    private void runNextThread(TimeSlice slice) {
	SchedulableObject thread = nextPendingThread();
	if (thread != null) {
	    thread.slice(slice);
	    if (CougaarThread.Debug)
		System.out.println("Starting " +thread+ " with slice " +slice);
	    thread.start();
	} else {
	    System.err.println("No threads on the queue!");
	}
    }


    // Parent offers us a slice
    public  boolean offerSlice(TimeSlice slice) {
	if (pendingThreadCount() > 0) {
	    runNextThread(slice);
	    return true;
	} else {
	    return false;
	}
    }



    private void releaseThreadSlice(SchedulableObject thread) {
	TimeSlice slice = thread.slice();
	thread.slice(null);
	releaseSlice(slice);
    }




    private void handoffSlice(SchedulableObject thread) {
	boolean expired = thread.slice().isExpired();
	boolean queue_empty = pendingThreadCount() == 0;
	if (expired) {
	    if (CougaarThread.Debug) 
		System.out.println(thread+ "'s slice expired");
	    releaseThreadSlice(thread);
	} else if (!queue_empty) {
	    // Reuse the slice
	    TimeSlice slice = thread.slice();
	    thread.slice(null);
	    noteChangeOfOwnership(slice);
	    runNextThread(slice);
	} else {
	    // No other threads to run
	    releaseThreadSlice(thread);
	}
    }
    


    void threadClaimed(SchedulableObject thread) {
	TimeSlice slice = thread.slice();
	if (slice != null) slice.run_start = System.currentTimeMillis();
	super.threadClaimed(thread);
    }
	

    // Called when a thread is about to end
    void threadReclaimed(SchedulableObject thread) {
	super.threadReclaimed(thread);
	handoffSlice(thread);
    }

    // Called when a thread is about to suspend.
    void suspendThread(SchedulableObject thread)
    {
	super.suspendThread(thread);
	handoffSlice(thread);
    }


    void resumeThread(SchedulableObject thread) {
	TimeSlice slice = thread.slice();
	if (slice != null) slice.run_start = System.currentTimeMillis();
	super.resumeThread(thread);
    }

    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(SchedulableObject thread) {
	SchedulableObject candidate = null;
	boolean expired = thread.slice().isExpired();
	if (expired) {
	    if (CougaarThread.Debug) 
		System.out.println(thread+ "'s slice expired");
	    // If our slice expired, just give up control
	    // without looking for another thread to yield to.
	    threadSuspended(thread);
	    releaseThreadSlice(thread);
	    return true;
	}

	// Slice is still good.  See if a queued thread is
	// available to yield to.  If so, give it our slice.

	if ( pendingThreadCount() == 0) {
	    // No pending threads
	    return false;
	}
		
	candidate = nextPendingThread(thread);
	if (candidate == thread) {
	    // No better-or-equal thread on the queue.
	    return false;
	}

	threadSuspended(thread);
	if (CougaarThread.Debug) 
	    System.out.println(thread+" giving slice to " +candidate);

	TimeSlice slice = thread.slice();
	noteChangeOfOwnership(slice);
	candidate.slice(slice);
		 
	    
	candidate.start();
	return true;
    }




    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    boolean maybeResumeThread(SchedulableObject thread)
    {
	TimeSlice slice = getSlice();
	if (slice != null) {
	    thread.slice(slice);
	    if (CougaarThread.Debug)
		System.out.println("Resuming " +thread+ " with slice " +slice);
	    resumeThread(thread);
	    return true;
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	    return false;
	}
    }

 

    void startOrQueue(SchedulableObject thread) {
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


    void changedMaxRunningThreadCount() {
	runMoreThreads();
    }

}
