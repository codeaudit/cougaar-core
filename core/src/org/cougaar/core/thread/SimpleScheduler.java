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


final class SimpleScheduler extends Scheduler
{
    SimpleScheduler(ThreadListenerProxy listenerProxy, String name) {
	super(listenerProxy, name);
    }


    private boolean canStartThread() {
	return runningThreadCount() < maxRunningThreadCount();
    }


    private void runNextThread() {
	nextPendingThread().start();
    }



    void changedMaxRunningThreadCount() {
	// Maybe we can run some pending threads
	while (canStartThread() && pendingThreadCount() > 0) {
	    runNextThread();
	}
    }
    

    // Called when a thread is about to end
    void threadReclaimed(ControllableThread thread) {
	super.threadReclaimed(thread);
	if (pendingThreadCount() > 0) runNextThread();
    }



    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(ControllableThread thread) {
	ControllableThread candidate = null;
	if (pendingThreadCount() == 0) {
	    // No point yielding since no pending threads
	    return false;
	}

	candidate = nextPendingThread(thread);
	if (candidate == thread) {
	    // No better-or-equal thread on the queue.
	    return false;
	}

	// We found a thread to yield to. 
	threadSuspended(thread);

	candidate.start();
	return true;
    }



    // Called when a thread is about to suspend.
    void suspendThread(ControllableThread thread) {
	super.suspendThread(thread);
	if (pendingThreadCount() > 0) runNextThread();
    }



    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    boolean maybeResumeThread(ControllableThread thread) {
	if (canStartThread()) {
	    resumeThread(thread);
	    return true;
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	    return false;
	}
    }

 

    void startOrQueue(ControllableThread thread) {
	if (canStartThread()) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
