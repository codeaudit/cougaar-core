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


final class Scheduler implements ThreadControlService 
{
    private static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    private static final int MaxRunningCountDefault = Integer.MAX_VALUE;

    private DynamicSortedQueue pendingThreads;
    private int maxRunningThreads;
    private int runningThreadCount = 0;
    private ThreadListenerProxy listenerProxy;

    private Comparator timeComparator =
	new Comparator() {
		public boolean equals(Object x) {
		    return x == this;
		}

		public int compare (Object x, Object y) {
		    long t1 = ((ControllableThread) x).timestamp();
		    long t2 = ((ControllableThread) y).timestamp();
		    if (t1 < t2)
			return -1;
		    else if (t1 > t2)
			return 1;
		    else
			return 0;
		}
	    };



    Scheduler(ThreadListenerProxy listenerProxy) {
	pendingThreads = new DynamicSortedQueue(timeComparator);
	maxRunningThreads = 
	    PropertyParser.getInt(MaxRunningCountProp, 
				  MaxRunningCountDefault);
	this.listenerProxy = listenerProxy;

    }

    // ThreadControlService 
    public synchronized void setQueueComparator(Comparator comparator)
    {
	pendingThreads.setComparator(comparator);
    }

    public synchronized void setMaxRunningThreadCount(int count) {
	maxRunningThreads = count;
	    
	// Maybe we can run some pending threads
	while (canStartThread() && !pendingThreads.isEmpty()) {
	    runNextThread();
	}
    }

    public int maxRunningThreadCount() {
	return maxRunningThreads;
    }

    public synchronized int pendingThreadCount() {
	return pendingThreads.size();
    }

    public synchronized int runningThreadCount() {
	return runningThreadCount;
    }


    public synchronized int activeThreadCount() {
	return runningThreadCount + pendingThreads.size();
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
	listenerProxy.notifyEnd(thread);
    }


    // Called when a thread has just started
    void threadStarted(ControllableThread thread) {
	synchronized (this) {
	    ++runningThreadCount; 
	}
	listenerProxy.notifyStart(thread);
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

    // Called when resuming a suspended or yielded thread that was
    // queued.
    void resumeQueuedThread(ControllableThread thread) {
	synchronized (this) {
	    ++runningThreadCount; 
	}
    }



 
    private void addPendingThread(ControllableThread thread) 
    {
	thread.stamp();
	listenerProxy.notifyPending(thread);
	pendingThreads.add(thread);
    }

    synchronized void startOrQueue(ControllableThread thread) {
	if (canStartThread()) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }

}
