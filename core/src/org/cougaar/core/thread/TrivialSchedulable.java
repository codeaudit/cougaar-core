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

import java.util.Timer;
import java.util.TimerTask;

final class TrivialSchedulable implements Schedulable
{
    private static final Timer timer = new Timer();

    private Object consumer;
    private Thread thread;
    private Runnable runnable;
    private String name;
    private int start_count;
    private boolean cancelled;
    private TimerTask task;

    TrivialSchedulable(Runnable runnable, 
		       String name,
		       Object consumer) 
    {
        this.runnable = runnable;
        if (name == null)
            this.name =  TrivialThreadPool.pool().generateName();
        else
            this.name = name;
        this.consumer = consumer;
	this.start_count = 0;
    }


    String getName() {
	return name;
    }

    public String toString() {
        return "<TrivialSchedulable " 
	    +(name == null ? "anonymous" : name)+ 
	    " for " +consumer+ ">";
    }

    public Object getConsumer() {
        return consumer;
    }

    // caller synchronizes
    private void thread_start() {
	start_count = 1; // forget any extra intervening start() calls
	thread = TrivialThreadPool.pool().getThread(this, runnable, name);
    }

    void thread_stop() {
	// If start_count > 1, start() was called while the
	// Schedulable was running.  Now that it's finished,  start it
	// again. 
	boolean restart = false;
	synchronized (this) {
	    restart = --start_count > 0;
	    thread = null;
	    if (restart) thread_start();
	}
    }

    public void start() {
        synchronized (this) {
	    // If the Schedulable has been cancelled, or has already
	    // been asked to start, there's nothing further to do.
            if (cancelled) return;
            if (++start_count > 1) return;
	    thread_start();
	}
    }

    
    private TimerTask task() {
	cancelTimer();
	task = new TimerTask() {
		public void run() {
		    start();
		}
	    };
	return task;
    }

    public synchronized void schedule(long delay) {
	timer.schedule(task(), delay);
    }


    public synchronized void schedule(long delay, long interval) 
    {
	timer.schedule(task(), delay, interval);
    }

    public synchronized void scheduleAtFixedRate(long delay, long interval)
    {
	timer.scheduleAtFixedRate(task(), delay, interval);
    }


    public synchronized void cancelTimer() 
    {
	if (task != null) task.cancel();
	task = null;
    }

    public synchronized int getState() {
        if (thread != null)
            return CougaarThread.THREAD_RUNNING;
        else
            return CougaarThread.THREAD_DORMANT;
    }

    public boolean cancel() {
        synchronized (this) {
	    cancelTimer();
            cancelled = true;
	    start_count = 0;
            if (thread != null) {
                // Currently running. 
                return false;
            } 
            return true;
        }

    }

}
