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

// Later this will move elsewhere...
package org.cougaar.core.thread;

import org.cougaar.core.service.ThreadService;
import org.cougaar.util.CircularQueue;

public class RunnableQueue implements Runnable
{
    private static final long  MAX_RUNTIME = 500;
    private CircularQueue queue;
    private Schedulable sched;
    
    public RunnableQueue(ThreadService svc, String name) 
    {
	queue = new CircularQueue();
	sched = svc.getThread(this, this, name);
    }

    public void add(Runnable runnable)
    {
	synchronized (queue) {
	    queue.add(runnable);
	}
	sched.start();
    }

    public void run()
    {
	long start = System.currentTimeMillis();
	Runnable next = null;
	boolean restart = false;
	while (true) {
	    synchronized (queue) {
		if (queue.isEmpty()) break;
		if (System.currentTimeMillis()-start > MAX_RUNTIME) {
		    // Spent too long in this thread but the queue
		    // isn't empty yet.  Start a new thread.
		    restart = true;
		    break;
		}
		next = (Runnable) queue.next();
	    }
	    next.run();
	}
	if (restart) sched.start();
    }
}

