/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
    private ThreadPool pool;
    private Scheduler scheduler;
    private ThreadPool.PooledThread thread;
    private Runnable runnable;
    private String name;
    private boolean restart;
    private boolean cancelled;
    private boolean queued;
    private boolean disqualified;

    SchedulableObject(TreeNode treeNode, 
		      Runnable runnable, 
		      String name,
		      Object consumer) 
    {
	this.pool = treeNode.getPool();
	this.scheduler = treeNode.getScheduler();
	this.runnable = runnable;
	if (name == null)
	    this.name =  pool.generateName();
	else
	    this.name = name;
	this.consumer = consumer;
    }


    Scheduler getScheduler() {
	return scheduler;
    }

    public String toString() {
	return "<Schedulable " +name+ " for " +consumer+ ">";
    }

    long getTimestamp() {
	return timestamp;
    }

    void setQueued(boolean flag) {
	queued = flag;
	if (flag) timestamp = System.currentTimeMillis();
    }

    boolean isQueued() {
	return queued;
    }


    boolean isDisqualified() {
	return disqualified;
    }

    void setDisqualified(boolean flag) {
	disqualified = flag;
	if (flag) queued = false;
    }

    public Object getConsumer() {
	return consumer;
    }



    void claim() {
	// thread has started or restarted
	scheduler.threadClaimed(this);
    }




    void reclaim() {
	// Notify listeners
	synchronized (this) { 
	    thread = null;
	}
	scheduler.threadReclaimed(this);
    }

    // Calback from the Reclaimer.
    void reclaimNotify() {
	scheduler.releaseRights(scheduler, this);
	if (restart) Starter.push(this);
    }

    void thread_start() {
	synchronized (this) {
	    restart = false;
	    queued = false;
	    restart = false;
	    thread = pool.getThread(runnable, name);
	    thread.start(this);
	}
    }

    public void start() {

	synchronized (this) {
	    if (cancelled) return;
	    if (thread != null) {
		// Currently running - set flag so it restarts itself
		// when the current run finishes
		restart = true;
		return;
	    }
	    
	}

	Starter.push(this);
    }


    public synchronized int getState() {
	// Later add a 'disqualified' state
	if (queued)
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
		// Currently running. 
		return false;
	    } 
	    if (queued) scheduler.dequeue(this);
	    queued = false;
	    return true;
	}

    }

}
