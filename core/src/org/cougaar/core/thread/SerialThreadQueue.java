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

import org.cougaar.util.CircularQueue;

final class SerialThreadQueue
{
	
    private CircularQueue schedulables;
    private Object lock;

    SerialThreadQueue()
    {
	schedulables = new CircularQueue();
	lock = new Object();
    }

    void listThreads(java.util.ArrayList records)
    {
	Object[] objects;
	TrivialSchedulable sched;
	ThreadStatusService.Record record;
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

    Object getLock()
    {
	return lock;
    }

    void enqueue(TrivialSchedulable sched) 
    {
	synchronized (lock) {
	    if (!schedulables.contains(sched)) {
		schedulables.add(sched);
		lock.notify();
	    }
	}
    }

    // caller synchronizes
    boolean isEmpty()
    {
	return schedulables.isEmpty();
    }

    // caller synchronizes
    TrivialSchedulable next() 
    {
	if (schedulables.isEmpty())
	    return null;
	else
	    return (TrivialSchedulable) schedulables.next();
    }


}



