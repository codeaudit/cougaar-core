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

import org.cougaar.util.ReusableThread;

/**
 * A special kind of ReusableThread which will notify listeners at
 * the beginning and end of the internal run method of the thread.
 */
final class ControllableThread extends ReusableThread
{
    private long timestamp;
    private Object consumer;
    private boolean suspended;
    private Object suspendLock;
    private ControllablePool pool;
    private TimeSlice slice;

    ControllableThread(ControllablePool pool) 
    {
	super(pool);
	this.suspendLock = new Object();
	this.pool = pool;
    }


    long timestamp() {
	return timestamp;
    }

    void stamp() {
	timestamp = System.currentTimeMillis();
    }

    Object consumer() {
	return consumer;
    }

    void consumer(Object consumer) {
	this.consumer = consumer;
    }


    TimeSlice slice() {
	return slice;
    }

    void slice(TimeSlice slice) {
	this.slice = slice;
    }


    protected void claim() {
	// thread has started or restarted
	super.claim();
	if (slice != null) slice.run_start = System.currentTimeMillis();
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
	try { sleep(millis); }
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
	    if (slice != null) slice.run_start = System.currentTimeMillis();
	}
    }


    protected void reclaim() {
	// thread is done
	pool.scheduler().threadReclaimed(this);
	setName( "Reclaimed " + getName());
	super.reclaim();
    }

    void thread_start() {
	pool.scheduler().threadStarting(this);
	super.start();
    }

    public void start() {
	if (suspended) {
	    synchronized (suspendLock) {
		suspendLock.notify();
		return;
	    }
	} else {
	    pool.scheduler().startOrQueue(this);
	}
    }

}
