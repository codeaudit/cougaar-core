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

import org.cougaar.util.CircularQueue;

final class SerialThreadRunner
{

    private CircularQueue schedulables;
    private Thread thread;
    private Object lock;
    private TrivialSchedulable current;

    SerialThreadRunner() 
    {
	schedulables = new CircularQueue();
	lock = new Object();
	thread = new Thread(new Body(), "Serial Thread Runner");
	thread.setDaemon(true);
	thread.start();
    }


    Thread getThread()
    {
	return thread;
    }

    void listThreads(ArrayList records)
    {
	Object[] objects;
	TrivialSchedulable sched;
	ThreadStatusService.Record record;
	sched = current;
	if (sched != null) {
	    record = new ThreadStatusService.RunningRecord();
	    try {
		Object consumer = sched.getConsumer();
		record.scheduler = "root";
		if (consumer != null) record.consumer = consumer.toString();
		record.schedulable = sched.getName();
		record.blocking_type = SchedulableStatus.NOT_BLOCKING;
		record.blocking_excuse = "none";
		// long startTime = thread.start_time;
		// record.elapsed = System.currentTimeMillis()-startTime;
		record.lane = sched.getLane();
		records.add(record);
	    } catch (Throwable t) {
		// ignore errors
	    }
	}
	synchronized (lock) {
	    objects = schedulables.toArray();
	}
	for (int i=0; i<objects.length; i++) {
	    sched = (TrivialSchedulable) objects[i];
	    record = new ThreadStatusService.QueuedRecord();
	    try {
		Object consumer = sched.getConsumer();
		record.scheduler = "root";
		if (consumer != null) record.consumer = consumer.toString();
		record.schedulable = sched.getName();
		record.blocking_type = SchedulableStatus.NOT_BLOCKING;
		record.blocking_excuse = "none";
		// long startTime = thread.start_time;
		// record.elapsed = System.currentTimeMillis()-startTime;
		record.lane = sched.getLane();
		records.add(record);
	    } catch (Throwable t) {
		// ignore errors
	    }
	}
    }

    void enqueue(TrivialSchedulable sched) 
    {
	synchronized (lock) {
	    if (!schedulables.contains(sched)) {
		schedulables.add(sched);
		lock.notify();
		// System.err.println("### Enqueueing schedulable " 
// 				   +sched+ ", " +schedulables.size()+
// 				   " items in the queue.");
	    } else {
		// System.err.println("### " +sched+ " is already in the queue");
	    }
	}
    }

    private void dequeue() 
    {
	while (true) {
	    synchronized (lock) {
		if (schedulables.isEmpty()) return;
		current = (TrivialSchedulable) schedulables.next();
	    }
	    current.getRunnable().run();
	    current.thread_stop();
	    current = null;
	}
    }

    private class Body implements Runnable 
    {
	public void run() 
	{
	    while (true) {
		dequeue();
		synchronized (lock) {
		    while (schedulables.isEmpty()) {
			try { 
			    lock.wait();
			    break;
			} catch (InterruptedException ex) {
			}
		    }
		}
	    }
	}
    }

}
