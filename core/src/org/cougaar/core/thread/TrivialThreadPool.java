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

import java.util.ArrayList;
import java.util.List;

class TrivialThreadPool
{
    private static TrivialThreadPool singleton = new TrivialThreadPool();
    static TrivialThreadPool pool() 
    {
	return singleton;
    }

    int anon_count = 0;
    ThreadRunner[] pool = new ThreadRunner[100];
    ArrayList list_pool = new ArrayList();

    synchronized String generateName() {
	return "TrivialThread-" + anon_count++;
    }

    private class ThreadRunner extends Thread {
	TrivialSchedulable schedulable;
	Runnable body;
	boolean in_use;
	Object lock = new Object();
	long start_time;

	ThreadRunner() 
	{
	    super("ThreadRunner");
	    setDaemon(true);
	    super.start();
	}

	public void start () {
	    throw new RuntimeException("You can't call start() on a PooledThread");
	}


	void configure(TrivialSchedulable schedulable,
		       Runnable body,
		       String name) 
	{
	    synchronized (lock) {
		this.schedulable = schedulable;
		this.body = body;
		// thread.setName(name);
		lock.notify();
	    }
	}

	public void run() {
	    while (true) {
		synchronized (lock) {
		    start_time = System.currentTimeMillis();
		    if (body != null) body.run();
		    if (schedulable != null) schedulable.thread_stop();
		    in_use = false;
		    try { lock.wait(); }
		    catch (InterruptedException ex) {} 
		}

	    }
	}
    }
    


    Thread getThread(TrivialSchedulable schedulable,
		     Runnable runnable, 
		     String name) 
    {
	ThreadRunner result = null;
	ThreadRunner candidate = null;

	synchronized (this) {
	    for (int i=0; i<pool.length; i++) {
		candidate = pool[i];
		if (candidate == null) {
		    result = new ThreadRunner();
		    pool[i] = result;
		    result.in_use = true;
		    break;
		} else if (!candidate.in_use) {
		    result = candidate;
		    result.in_use = true;
		    break;
		}
	    }

	    if (result == null && list_pool != null) {
		// Use the slow ArrayList.
		Object[] list_array = list_pool.toArray();
		for (int i=0; i<list_array.length; i++) {
		    candidate = (ThreadRunner) list_array[i];
		    if (!candidate.in_use) {
			result = candidate;
			result.in_use = true;
			break;
		    }
		}
	    }
	    
	    if (result == null) {
		// None in the list either. Make one and add it,
		result = new ThreadRunner();
		result.in_use = true;
		list_pool.add(result);
	    }
	}


	result.configure(schedulable, runnable, name);

	return result;
    }


    void listRunningThreads(List records) 
    {
	ThreadRunner thread = null;
	for (int i=0; i<pool.length; i++) {
	    thread = pool[i];
	    addRecord(thread, records);
	}
	if (list_pool != null) {
	    Object[] list_array = list_pool.toArray();
	    for (int i=0; i<list_array.length; i++) {
		thread = (ThreadRunner) list_array[i];
		addRecord(thread, records);
	    }
	}
    }

    void addRecord(ThreadRunner thread, List records) 
    {
	if (thread != null && thread.in_use) {
	    ThreadStatusService.Record record = 
		new ThreadStatusService.RunningRecord();
	    try {
		TrivialSchedulable sched = thread.schedulable;
		Object consumer = sched.getConsumer();
		record.scheduler = "root";
		if (consumer != null) record.consumer = consumer.toString();
		record.schedulable = sched.getName();
		record.blocking_type = SchedulableStatus.NOT_BLOCKING;
		record.blocking_excuse = "none";
		long startTime = thread.start_time;
		record.elapsed = System.currentTimeMillis()-startTime;
		records.add(record);
	    } catch (Throwable t) {
		// ignore errors
	    }
	}
    }

}
