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

import org.cougaar.util.ReusableThreadPool;
import org.cougaar.util.ReusableThread;
import org.cougaar.util.PropertyParser;

/**
 * A special kind of ReusableThreadPool which makes
 * ControllableThreads.
 */
final class ControllablePool extends ReusableThreadPool 
{

    private static final String InitialPoolSizeProp =
	"org.cougaar.thread.poolsize.initial";
    private static final int InitialPoolSizeDefault = 32;
    private static final String MaxPoolSizeProp =
	"org.cougaar.thread.poolsize.max";
    private static final int MaxPoolSizeDefault = 64;


    private Scheduler scheduler;

    ControllablePool(ThreadGroup group, Scheduler scheduler)  {
	super(group, 
	      PropertyParser.getInt(InitialPoolSizeProp, 
				    InitialPoolSizeDefault),
	      PropertyParser.getInt(MaxPoolSizeProp, 
				    MaxPoolSizeDefault));
	this.scheduler = scheduler;
    }

    protected ReusableThread constructReusableThread() {
	return  new ControllableThread(this);
    }

    Scheduler scheduler() {
	return scheduler;
    }

}

