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
    SimpleScheduler(ThreadListenerProxy listenerProxy) {
	super(listenerProxy);
    }


    void changedMaxRunningThreadCount() {
	// Maybe we can run some pending threads
	while (canStartThread() && !pendingThreads.isEmpty()) {
	    runNextThread();
	}
    }
    

    private boolean canStartThread() {
	return runningThreadCount < maxRunningThreads;
    }


    private void runNextThread() {
	((ControllableThread)pendingThreads.next()).start();
    }


    // Called when a thread is about to end
    void threadEnded(ControllableThread thread) {
	synchronized (this) {
	    --runningThreadCount; 
	    if (!pendingThreads.isEmpty()) runNextThread();
	}
	super.threadEnded(thread);
    }



    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    boolean maybeYieldThread(ControllableThread thread) {
	ControllableThread candidate = null;
	synchronized (this) {
	    if (pendingThreads.isEmpty()) {
		// No point yielding since no pending threads
		return false;
	    }

	    candidate = (ControllableThread) pendingThreads.next(thread);
	    if (candidate == thread) {
		// No better-or-equal thread on the queue.
		return false;
	    }

	    // We found a thread to yield to. 
	    --runningThreadCount; 
	}
	candidate.start();
	return true;
    }



    // Called when a thread is about to suspend.
    synchronized void suspendThread(ControllableThread thread) {
	--runningThreadCount; 
	if (!pendingThreads.isEmpty()) runNextThread();
    }


    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    synchronized boolean maybeResumeThread(ControllableThread thread) {
	if (canStartThread()) {
	    ++runningThreadCount;
	    return true;
	} else {
	    // couldn'resume - place the thread back on the queue
	    addPendingThread(thread);
	    return false;
	}
    }

 

    synchronized void startOrQueue(ControllableThread thread) {
	if (canStartThread()) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
