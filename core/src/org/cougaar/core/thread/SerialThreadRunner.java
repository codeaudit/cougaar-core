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

final class SerialThreadRunner
{
    private Thread thread;
    private TrivialSchedulable current;
    private SerialThreadQueue queue;

    SerialThreadRunner(SerialThreadQueue queue) 
    {
	this.queue = queue;
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
    }


    private void dequeue() 
    {
	while (true) {
	    synchronized (queue.getLock()) {
		current = queue.next();
	    }
	    if (current == null) return;
	    
	    current.getRunnable().run();
	    current.thread_stop();
	    current = null;
	}
    }

    private class Body implements Runnable 
    {
	public void run() 
	{
	    Object lock = queue.getLock();
	    while (true) {
		dequeue();
		synchronized (lock) {
		    while (queue.isEmpty()) {
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
