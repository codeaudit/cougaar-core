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

/**
 * The serializing trivial implementation of Thread Service.  It
 * consists of a single @link SerialThreadRunner and @link
 * SerialThreadQueue.
 */
final class SingleThreadServiceProxy
    extends TrivialThreadServiceProxy
{
    private static final int NUMBER_OF_RUNNERS = 1;

    private static SerialThreadQueue queue;
    private static SerialThreadRunner[] runners;
    
    static {
	queue = new SerialThreadQueue();
	runners = new SerialThreadRunner[NUMBER_OF_RUNNERS];
	for (int i=0; i<runners.length; i++)
	    runners[i] = new SerialThreadRunner(queue);
    }
	

    SingleThreadServiceProxy()
    {
	super();
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name) 
    {
	return new SerialSchedulable(runnable, name, consumer, queue);
    }

    int iterateOverThreads(ThreadStatusService.Body body)
    {
	int count = 0;
	for (int i=0; i<runners.length; i++) 
	    count += runners[i].iterateOverThreads(body);
	count += queue.iterateOverThreads(body);
	return count;
    }

}
