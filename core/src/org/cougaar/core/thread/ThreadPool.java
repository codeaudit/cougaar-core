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

import java.util.ArrayList;
import java.util.Iterator;

import org.cougaar.util.PropertyParser;

public class ThreadPool 
{

    private static ThreadPool SharedPool;

    static synchronized ThreadPool getPool(ThreadGroup group) {
	// return new ThreadPool(group);
	if (SharedPool == null) SharedPool = new ThreadPool();
	return SharedPool;
    }



    static class PooledThread extends Thread {
	private SchedulableObject schedulable;

	private boolean in_use = false;

	/** reference to our thread pool so we can return when we die **/
	private ThreadPool pool;
  
	/** our runnable object, or null if we haven't been assigned one **/
	private Runnable runnable = null;
  
	/** Has this thread already be actually started yet?
	 * access needs to be guarded by runLock.
	 **/
	private boolean isStarted = false;

	/** are we actively running the runnable? **/
	private boolean isRunning = false;

	/** guards isRunning, synced while actually executing and waits when
	 * suspended.
	 **/
	private Object runLock = new Object();

	public void setRunnable(Runnable r) {
	    runnable = r;
	}
	protected Runnable getRunnable() {
	    return runnable;
	}

	SchedulableObject getSchedulable() {
	    return schedulable;
	}

	/** The only constructor. **/
	public PooledThread(ThreadPool p) {
	    super(p.getThreadGroup(), null, "PooledThread");
	    setDaemon(true);
	    pool = p;
	}

	// Hook for subclasses
	private void claim() {
	    schedulable.claim();
	}

	public final void run() {
	    while (true) {
		synchronized (runLock) {
		    claim();
		    Runnable r = getRunnable();
		    if (r != null)
			r.run();
		    isRunning = false;

		    reclaim();
		    Reclaimer.push(schedulable);
		    try {
			runLock.wait();       // suspend
		    } catch (InterruptedException ie) {}
		}
	    }
	}

	public void start (SchedulableObject schedulable) {
	    this.schedulable = schedulable;
	    start();
	}

	public void start() throws IllegalThreadStateException {
	    synchronized (runLock) {
		if (isRunning) 
		    throw new IllegalThreadStateException("PooledThread already started: "+
							  this);
		isRunning = true;

		if (!isStarted) {
		    isStarted=true;
		    super.start();
		} else {
		    runLock.notify();     // resume
		}
	    }
	}

	private void reclaim() {
	    schedulable.reclaim();
	    setName( "Reclaimed " + getName());
	    in_use = false;
	}
    }
    


    /** initial number of PooledThreads in the pool **/
    private static int defaultInitialPoolSize;

    /** maximum number of unused PooledThreads to keep in the pool **/
    private static int defaultMaximumPoolSize;

    /** initialize initialPoolSize and maximumPoolSize from system,
     * properties and create the default ThreadPool from these
     * values.
     */
    private static final String InitialPoolSizeProp =
	"org.cougaar.thread.poolsize.initial";
    private static final int InitialPoolSizeDefault = 10;
    private static final String MaxPoolSizeProp =
	"org.cougaar.thread.poolsize.max";
    private static final int MaxPoolSizeDefault = 64;

    static {
	defaultInitialPoolSize = 
	    PropertyParser.getInt(InitialPoolSizeProp, 
				  InitialPoolSizeDefault);
	defaultMaximumPoolSize = 
	    PropertyParser.getInt(MaxPoolSizeProp, MaxPoolSizeDefault);

    }

    /** The ThreadGroup of the pool - all threads in the pool must be
     * members of the same threadgroup.
     **/
    private ThreadGroup group;
    /** The maximum number of unused threads to keep around in the pool.
     * anything beyond this may be destroyed or GCed.
     **/
    private int maximumSize;

    /** the actual pool **/
    private PooledThread pool[];
    private ArrayList extra;

    public ThreadPool() {
	this(defaultInitialPoolSize, defaultMaximumPoolSize);
    }

    public ThreadPool(ThreadGroup group) {
	this(group, defaultInitialPoolSize, defaultMaximumPoolSize);
    }

    public ThreadPool(int initial, int maximum) {
	this(Thread.currentThread().getThreadGroup(), initial, maximum);
    }

    public ThreadPool(ThreadGroup group, int initial, int maximum) {
	this.group = group;
	if (initial > maximum) initial = maximum;
	maximumSize = maximum;
	pool = new PooledThread[maximum];
	for (int i = 0 ; i < initial; i++) pool[i] = constructReusableThread();
	extra = new ArrayList(maximum);
    }


    public ThreadGroup getThreadGroup() {
	return group;
    }

    public PooledThread getThread() {
	return getThread(null, "PooledThread");
    }

    public PooledThread getThread(String name) {
	return getThread(null, name);
    }

    public PooledThread getThread(Runnable runnable) {
	return getThread(runnable, "PooledThread");    
    }
  
    public PooledThread getThread(Runnable runnable, String name) {
	PooledThread thread = null;

	synchronized (this) {
	    // Check fast fixed-size list first
	    for (int i=0; i<maximumSize; i++) {
		PooledThread candidate = pool[i];
		if (candidate == null) {
		    // System.err.println("New thread " + i);
		    thread = constructReusableThread();
		    pool[i] = thread;
		    thread.in_use = true;
		    break;
		} else if (!candidate.in_use) {
		    // System.err.println("Using thread " + i);
		    thread = candidate;
		    thread.in_use = true;
		    break;
		}
	    }

	    if (thread == null) {
		// No luck. check slow dynamic list
		Iterator i = extra.iterator();
		while (i.hasNext()) {
		    PooledThread candidate = (PooledThread) i.next();
		    if (!candidate.in_use) {
			thread = candidate;
			thread.in_use = true;
			break;
		    }

		}
	    }

	    if (thread == null) {
		// None available.  Make one that we'll never actually
		// reuse.  We need some cleanup here when this finishes.
		thread = constructReusableThread();
		extra.add(thread);
	    
	    }
	}

	thread.setRunnable(runnable);
	thread.setName(name);

	return thread;
    }
  
    /** actually construct a new PooledThread **/
    protected PooledThread constructReusableThread() {
	return new PooledThread(this);
    }



}
