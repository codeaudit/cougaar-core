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

import org.cougaar.core.service.ThreadListenerService;

/**
 * Proxy for ThreadListenerService
 */
final class ThreadListenerProxy implements ThreadListenerService
{
    private ArrayList listeners;

    ThreadListenerProxy() {
	listeners = new ArrayList();
    }
		    
    synchronized void notifyQueued(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
        for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadQueued(schedulable, consumer);
	}
    }

    synchronized void notifyDequeued(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadDequeued(schedulable, consumer);
	}
    }

    synchronized void notifyStart(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadStarted(schedulable, consumer);
	}
    }

    synchronized void notifyEnd(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadStopped(schedulable, consumer);
	}
    }

    synchronized void notifyRightGiven(Scheduler scheduler) {
	String id = scheduler.getName();
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.rightGiven(id);
	}
    }

    synchronized void notifyRightReturned(Scheduler scheduler) {
	String id = scheduler.getName();
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.rightReturned(id);
	}
    }




    public synchronized void addListener(ThreadListener listener) {
	listeners.add(listener);
    }


    public synchronized void removeListener(ThreadListener listener) {
	listeners.remove(listener);
    }

}
