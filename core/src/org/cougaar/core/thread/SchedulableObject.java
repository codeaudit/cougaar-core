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


final class SchedulableObject implements Schedulable
{
    private long timestamp;
    private Object consumer;
    private boolean suspended;
    private Object suspendLock;
    private SchedulableThreadPool pool;
    private TimeSlice slice;
    private SchedulableThread thread;
    private Runnable runnable;
    private String name;
    private boolean restart;
    private boolean cancelled;
    private boolean queued;

    SchedulableObject(SchedulableThreadPool pool, 
		      Runnable runnable, 
		      String name,
		      Object consumer) 
    {
	this.suspendLock = new Object();
	this.pool = pool;
	this.runnable = runnable;
	this.name = name;
	this.consumer = consumer;
    }


    long timestamp() {
	return timestamp;
    }

    void notifyPending() {
	queued = true;
	timestamp = System.currentTimeMillis();
    }

    Object consumer() {
	return consumer;
    }


    TimeSlice slice() {
	return slice;
    }

    void slice(TimeSlice slice) {
	this.slice = slice;
    }


    void claim() {
	// thread has started or restarted
	pool.scheduler().threadClaimed(this);
    }


    // The argument is only here to avoid overriding yield(),
    void yield(Object ignore) {
	boolean yielded = pool.scheduler().maybeYieldThread(this);
	if (yielded) attemptResume();
    }

    // Must be called from a block that's synchronized on lock.
    void wait(Object lock, long millis) {
	pool.scheduler().suspendThread(this);
	try { lock.wait(millis); }
	catch (InterruptedException ex) {}
	attemptResume();
    }

    // Must be called from a block that's synchronized on lock.
    void wait(Object lock) {
	pool.scheduler().suspendThread(this);
	try { lock.wait(); }
	catch (InterruptedException ex) {}
	attemptResume();
    }


    void suspend(long millis) {
	pool.scheduler().suspendThread(this);
	try { thread.sleep(millis); }
	catch (InterruptedException ex) {}
	attemptResume();
    }



    private void attemptResume() {
	suspended = true;
	synchronized (suspendLock) {
	    suspended = !pool.scheduler().maybeResumeThread(this);
	    if (suspended) {
		// Couldn't be resumed - requeued instead
		while (true) {
		    try {
			// When the thread is pulled off the
			// queue, a notify will wake up this wait.
			suspendLock.wait();
			break;
		    } catch (InterruptedException ex) {
		    }
		}
		pool.scheduler().resumeThread(this);
		suspended = false;
	    }
	}
    }


    void reclaim() {
	// thread is done
	boolean again = false;
	synchronized (this) { 
	    thread = null;  
	    again = restart;
	}
	pool.scheduler().threadReclaimed(this);
	if (again) pool.scheduler().startOrQueue(this);
    }

    void thread_start() {
	pool.scheduler().threadStarting(this);
	synchronized (this) {
	    restart = false;
	    queued = false;
	    thread = (SchedulableThread) pool.getThread(runnable, name);
	    thread.start(this);
	}
    }

    public void start() {

	if (suspended) {
	    synchronized (suspendLock) {
		suspendLock.notify();
		return;
	    }
	}
	
	synchronized (this) {
	    if (cancelled) return;
	    if (thread != null) {
		// Currently running - set flag so it restarts itself
		// when the current run finishes
		restart = true;
		return;
	    }
	}

	pool.scheduler().startOrQueue(this);
    }


    public synchronized int getState() {
	if (suspended) 
	    return CougaarThread.THREAD_SUSPENDED;
	else if (queued)
	    return CougaarThread.THREAD_PENDING;
	else if (thread != null)
	    return CougaarThread.THREAD_RUNNING;
	else
	    return CougaarThread.THREAD_DORMANT;
    }

    public boolean cancel() {
	synchronized (this) {
	    cancelled = true;
	    restart = false;
	    if (thread != null) {
		// Currently running.  Do we need to do anything
		// special if it's suspended?
		if (suspended) thread.interrupt();
		return false;
	    } 
	    if (queued) pool.scheduler().dequeue(this);
	    queued = false;
	    return true;
	}

    }

}
