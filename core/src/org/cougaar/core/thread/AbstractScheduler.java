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


abstract class AbstractScheduler implements ThreadControlService 
{
    static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    static final int MaxRunningCountDefault = Integer.MAX_VALUE;

    protected DynamicSortedQueue pendingThreads;
    protected int maxRunningThreads;
    protected int runningThreadCount = 0;
    protected ThreadListenerProxy listenerProxy;
    protected ThreadServiceProxy proxy;

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



    AbstractScheduler(ThreadListenerProxy listenerProxy) {
	pendingThreads = new DynamicSortedQueue(timeComparator);
	maxRunningThreads = 
	    PropertyParser.getInt(MaxRunningCountProp, 
				  MaxRunningCountDefault);
	this.listenerProxy = listenerProxy;

    }


    void setProxy(ThreadServiceProxy proxy) {
	this.proxy = proxy;
    }

    // ThreadControlService 
    public synchronized void setQueueComparator(Comparator comparator)
    {
	pendingThreads.setComparator(comparator);
    }

    public synchronized void setMaxRunningThreadCount(int count) {
	maxRunningThreads = count;
	changedMaxRunningThreadCount();
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



    void wakeup (){
    }

    void addPendingThread(ControllableThread thread) 
    {
	thread.stamp();
	listenerProxy.notifyPending(thread);
	pendingThreads.add(thread);
    }


    // Called when a thread is about to end
    void threadEnded(ControllableThread thread) {
	listenerProxy.notifyEnd(thread);
    }

    // Called when a thread has just started
    void threadStarted(ControllableThread thread) {
	synchronized (this) {
	    ++runningThreadCount; 
	}
	listenerProxy.notifyStart(thread);
    }



    // Called when resuming a suspended or yielded thread that was
    // queued.
    void resumeQueuedThread(ControllableThread thread) {
	synchronized (this) {
	    ++runningThreadCount; 
	}
    }



    void changedMaxRunningThreadCount() {
    }




    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    abstract boolean maybeYieldThread(ControllableThread thread);

    // Called when a thread is about to suspend.
    abstract void suspendThread(ControllableThread thread);

    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    abstract boolean maybeResumeThread(ControllableThread thread);

    abstract void startOrQueue(ControllableThread thread);

}
