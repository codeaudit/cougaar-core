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

import org.cougaar.core.service.ThreadService;

/**
 * The trivial implementation of Thread Service.
 */
final class TrivialThreadServiceProxy 	implements ThreadService
{
    TrivialThreadServiceProxy() 
    {
    }

    public Schedulable getThread(Object consumer, Runnable runnable) 
    {
	return new TrivialSchedulable(runnable, null, consumer);
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name) 
    {
	return new TrivialSchedulable(runnable, name, consumer);
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name,
				 int lane) 
    {
	return new TrivialSchedulable(runnable, name, consumer);
    }


    // No longer supported
    public void schedule(java.util.TimerTask task, long delay) 
    {
	throw new RuntimeException("ThreadService.schedule is no longer supported");
    }


    public void schedule(java.util.TimerTask task, long delay, long interval) 
    {
	throw new RuntimeException("ThreadService.schedule is no longer supported");
    }

    public void scheduleAtFixedRate(java.util.TimerTask task, 
				    long delay, 
				    long interval)
    {
	throw new RuntimeException("ThreadService.scheduleAtFixedRate is no longer supported");
    }


}
