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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;

import java.util.ArrayList;
import java.util.TimerTask;

/**
 * The proxy implementation of Thread Service.
 */
final class ThreadServiceProxy 	implements ThreadService
{
    private ControllablePool pool;
    private ThreadGroup group;
    private TimerRunnable timer;
    private ThreadServiceProxy parent;
    private ArrayList children;

    ThreadServiceProxy(ServiceBroker sb,  String name, Scheduler scheduler) 
    {
	parent = (ThreadServiceProxy)
	    sb.getService(this, ThreadService.class, null);

	children = new ArrayList();
	if (parent == null) {
	    group = new ThreadGroup(name);
	} else {
	    parent.addChild(this);
	    group = new ThreadGroup(parent.group, name);
	}

	pool = new ControllablePool(group, scheduler);


	timer = new TimerRunnable(this);

	// Use a special Thread for the timer
	Thread thread = new Thread(group, timer, name+"Timer");
	thread.setDaemon(true);
	thread.start();
    }

    private synchronized void addChild(ThreadServiceProxy child) {
	children.add(child);
    }

    private Thread consumeThread(Thread thread,  Object consumer) {
	((ControllableThread) thread).consumer(consumer);
	return thread;
    }





    public Thread getThread(Object consumer, Runnable runnable) {
	return consumeThread(pool.getThread(runnable),  consumer);
    }

    public Thread getThread(Object consumer, 
			    Runnable runnable, 
			    String name) 
    {
	return consumeThread(pool.getThread(runnable, name), 
			     consumer);
    }


    public TimerTask getTimerTask(Object consumer, Runnable runnable) {
	return timer.getTimerTask(consumer, runnable);
    }


    public TimerTask getTimerTask(Object consumer, 
				  Runnable runnable,
				  String name) 
    {
	return timer.getTimerTask(consumer, runnable, name);
    }

    public void schedule(TimerTask task, long delay) {
	timer.schedule(task, delay);
    }


    public void schedule(TimerTask task, long delay, long interval) {
	timer.schedule(task, delay, interval);
    }

    public void scheduleAtFixedRate(TimerTask task, 
				    long delay, 
				    long interval)
    {
	timer.scheduleAtFixedRate(task, delay, interval);
    }


    public void suspendCurrentThread(long millis) {
	Thread thread = Thread.currentThread();
	if (thread instanceof ControllableThread) {
	    ((ControllableThread) thread).suspend(millis);
	}
    }


    public void yieldCurrentThread() {
	Thread thread = Thread.currentThread();
	if (thread instanceof ControllableThread) {
	    ((ControllableThread) thread).yield(null);
	}
    }
    public void blockCurrentThread(Object lock, long millis) {
	Thread thread = Thread.currentThread();
	if (thread instanceof ControllableThread) {
	    ((ControllableThread) thread).wait(lock, millis);
	}
    }
    public void blockCurrentThread(Object lock) {
	Thread thread = Thread.currentThread();
	if (thread instanceof ControllableThread) {
	    ((ControllableThread) thread).wait(lock);
	}
    }


}
