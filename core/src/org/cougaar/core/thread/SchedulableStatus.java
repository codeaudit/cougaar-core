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

public final class SchedulableStatus
{
    public static final int NOT_BLOCKING = -1;
    public static final int OTHER = 0;
    public static final int WAIT = 1;
    public static final int FILEIO = 2;
    public static final int NETIO = 3;

    public static void beginBlocking(int type, String excuse) 
    {
	Thread thread = Thread.currentThread();
	if (thread instanceof ThreadPool.PooledThread) {
	    ThreadPool.PooledThread pthread = (ThreadPool.PooledThread) thread;
	    SchedulableObject sched = pthread.getSchedulable();
	    sched.setBlocking(type, excuse == null ? "No excuse given" : excuse);
	}
    }

    public static void beginWait(String excuse) {
	beginBlocking(WAIT, excuse);
    }

    public static void beginFileIO(String excuse) {
	beginBlocking(FILEIO, excuse);
    }

    public static void beginNetIO(String excuse) {
	beginBlocking(NETIO, excuse);
    }

    public static void endBlocking() 
    {
	Thread thread = Thread.currentThread();
	if (thread instanceof ThreadPool.PooledThread) {
	    ThreadPool.PooledThread pthread = (ThreadPool.PooledThread) thread;
	    SchedulableObject sched = pthread.getSchedulable();
	    sched.clearBlocking();
	}
    }

  public static void withBlocking(int type, String excuse, Runnable thunk) {
    try {
      beginBlocking(type, excuse);
      thunk.run();
    } finally {
      endBlocking();
    }
  }
  public static void withWait(String excuse, Runnable thunk) {
    withBlocking(WAIT, excuse, thunk);
  }
  public static void withFileIO(String excuse, Runnable thunk) {
    withBlocking(FILEIO, excuse, thunk);
  }
  public static void withNetIO(String excuse, Runnable thunk) {
    withBlocking(NETIO, excuse, thunk);
  }


    public static String statusString(int type, String excuse)
    {
	String string = excuse;
	switch (type) {
	case NOT_BLOCKING:
	    string = "none";
	    break;
	case OTHER:
	    break;
	case WAIT:
	    string = "Lock wait: " + string;
	    break;
	case FILEIO:	
	    string = "Disk I/O: " + string;
	    break;
	case NETIO:
	    string = "Network I/O: " + string;
	    break;
	}
	return string;
    }

}
