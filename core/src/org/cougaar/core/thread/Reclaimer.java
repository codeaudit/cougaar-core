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

import org.cougaar.util.CircularQueue;

final class Reclaimer extends Thread
{

    private static Reclaimer singleton;

    static {
	singleton = new Reclaimer();
	singleton.start();
    }

    static void push(SchedulableObject schedulable) {
	synchronized (singleton.lock) {
	    singleton.queue.add(schedulable);
	    singleton.lock.notify();
	}
    }


    private CircularQueue queue;
    private Object lock;

    private Reclaimer() {
	super("Scheduler Reclaimer");
	setDaemon(true);
	queue = new CircularQueue();
	lock = new Object();
    }

    private void dequeue() {
	SchedulableObject schedulable = null;
	while (true) {
	    synchronized (lock) {
		if (queue.isEmpty()) return;
		schedulable = (SchedulableObject) queue.next();
	    }
	    schedulable.reclaimNotify();
	}
    }

    public void run() {
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
