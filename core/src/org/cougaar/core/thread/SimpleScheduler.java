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


class SimpleScheduler extends Scheduler
{
    SimpleScheduler(ThreadListenerProxy listenerProxy, String name) {
	super(listenerProxy, name);
    }

    
    SchedulableObject getNextPending() {
	return nextPendingThread();
    }


    synchronized boolean requestRights(SimpleScheduler requestor) {
	if (maxRunningThreads <= 0) return true;
	if (runningThreadCount < maxRunningThreads) {
	    ++runningThreadCount;
	    // System.err.println("Grabbed one: " +runningThreadCount);
	    return true;
	}
	return false;
    }

    synchronized void releaseRights(SimpleScheduler consumer) {
	SchedulableObject handoff = getNextPending();
	if (handoff != null) {
	    // System.err.println("Reused one: " +runningThreadCount);
	    handoff.thread_start();
	} else {
	    --runningThreadCount;
	    // System.err.println("Released one: " +runningThreadCount);
	}
    }




    // Called when a thread is about to end
    void threadReclaimed(SchedulableObject thread) {
	super.threadReclaimed(thread);
	releaseRights(this);
    }




    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(SchedulableObject thread) {
	SchedulableObject handoff = nextPendingThread(thread);
	if (handoff == thread) {
	    // No better-or-equal thread on the queue.
	    return false;
	}

	// We found a thread to yield to. 
	threadSuspended(thread);

	handoff.thread_start();
	return true;
    }



    // Called when a thread is about to suspend.
    void suspendThread(SchedulableObject thread) {
	super.suspendThread(thread);
	releaseRights(this);
    }



    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    boolean maybeResumeThread(SchedulableObject thread) {
	boolean can_run = requestRights(this);
	if (can_run) {
	    resumeThread(thread);
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	}
	return can_run;
    }

 

    void startOrQueue(SchedulableObject thread) {
	// If the queue isn't empty, queue this one too.
	synchronized (this) {
	    if (pendingThreadCount() > 0) {
		addPendingThread(thread);
		return;
	    }
	}

	boolean can_run = requestRights(this);
	if (can_run) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
