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

public class CougaarThread
{

    // Four states: running, suspended, pending, dormant
    public static final int THREAD_RUNNING = 0;
    public static final int THREAD_SUSPENDED = 1;
    public static final int THREAD_PENDING = 2;
    public static final int THREAD_DORMANT = 3;

    public static final boolean Debug = 
	Boolean.getBoolean("org.cougaar.thread.debug");

    
    private static SchedulableObject getSchedulableObject() {
	Thread thread = Thread.currentThread();
	if (thread instanceof SchedulableThread) 
	    return ((SchedulableThread) thread).getSchedulable();
	else
	    return null;
		    
    }

    public static void sleep(long millis) {
	SchedulableObject sched =  getSchedulableObject();
	if (sched != null)  sched.suspend(millis);
    }


    public static void yield() {
	SchedulableObject sched =  getSchedulableObject();
	if (sched != null)  sched.yield(null);
    }

    public static void wait(Object lock, long millis) {
	SchedulableObject sched =  getSchedulableObject();
	if (sched != null)  sched.wait(lock, millis);
    }

    public static void wait(Object lock) {
	SchedulableObject sched =  getSchedulableObject();
	if (sched != null)  sched.wait(lock);
    }

}
