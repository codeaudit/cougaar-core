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
import java.util.Iterator;
import java.util.List;

import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

class ThreadPool 
{

    private static ThreadPool SharedPool;

    static synchronized ThreadPool getPool(ThreadGroup group) {
	// return new ThreadPool(group);
	if (SharedPool == null) SharedPool = new ThreadPool();
	return SharedPool;
    }



    static final class PooledThread extends Thread {
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

	private long start_time;

	private void setRunnable(Runnable r) {
	    runnable = r;
	}

	SchedulableObject getSchedulable() {
	    return schedulable;
	}

	/** The only constructor. **/
	private PooledThread(ThreadPool p, String name) {
	    super(p.getThreadGroup(), null, name);
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
		    if (runnable != null) runnable.run();

		    runnable = null;
		    isRunning = false;
		    reclaim();
		    Reclaimer.push(schedulable);
		    
		    schedulable = null;
		    try {
			runLock.wait();       // suspend
		    } catch (InterruptedException ie) {}
		}
	    }
	}

	public void start () {
	    throw new RuntimeException("You can't call start() on a PooledThread");
	}

	void start(SchedulableObject schedulable) 
	    throws IllegalThreadStateException 
	{
	    synchronized (runLock) {
		if (isRunning) 
		    throw new IllegalThreadStateException("PooledThread already started: "+
							  schedulable);
		this.schedulable = schedulable;
		start_time = System.currentTimeMillis();
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
	    if (pool.logger.isInfoEnabled()) setName( "Reclaimed");
	    in_use = false;
	}
    }
    


    private static final String InitialPoolSizeProp =
	"org.cougaar.thread.poolsize.initial";
    private static final int InitialPoolSizeDefault = 10;


    /** The ThreadGroup of the pool - all threads in the pool must be
     * members of the same threadgroup.
     **/
    private ThreadGroup group;
    /** The maximum number of unused threads to keep around in the pool.
     * anything beyond this may be destroyed or GCed.
     **/

    /** the actual pool **/
    private PooledThread pool[];
    private ArrayList list_pool;
    private Logger logger;
    private int index = 0;

    ThreadPool() {
	int initialSize = PropertyParser.getInt(InitialPoolSizeProp, 
						InitialPoolSizeDefault);
	int maximumSize = PropertyParser.getInt(Scheduler.MaxRunningCountProp, 
					    Scheduler.MaxRunningCountDefault);
	group = Thread.currentThread().getThreadGroup();
	logger = Logging.getLogger(getClass().getName());
	if (maximumSize < 0) {
	    // Unlimited.  Make an array of a somewhat arbitrary size
	    // (100 or initialSize, whichever is larger), and also
	    // make an ArrayList which will be used if the array runs
	    // out.
	    pool = new PooledThread[Math.max(initialSize, 100)];
	    list_pool = new ArrayList(100);
	} else {
	    if (initialSize > maximumSize) initialSize = maximumSize;
	    pool = new PooledThread[maximumSize];
	}
	for (int i = 0 ; i < initialSize; i++)
	    pool[i] = constructReusableThread();
    }


    private synchronized String nextName() {
	return "CougaarPooledThread-" + (index++);
    }

    ThreadGroup getThreadGroup() {
	return group;
    }

    PooledThread getThread(Runnable runnable, String name) {
	PooledThread thread = null;
	PooledThread candidate = null;

	synchronized (this) {
	    for (int i=0; i<pool.length; i++) {
		candidate = pool[i];
		if (candidate == null) {
		    thread = constructReusableThread();
		    pool[i] = thread;
		    thread.in_use = true;
		    break;
		} else if (!candidate.in_use) {
		    thread = candidate;
		    thread.in_use = true;
		    break;
		}
	    }

	    if (thread == null && list_pool != null) {
		// Use the slow ArrayList.  This is only enabled if
		// there's no thread limit.
		Iterator itr  = list_pool.iterator();
		while (itr.hasNext()) {
		    candidate = (PooledThread) itr.next();
		    if (!candidate.in_use) {
			thread = candidate;
			thread.in_use = true;
			break;
		    }
		}
		// None in the list either. Make one and add it,
		thread = constructReusableThread();
		thread.in_use = true;
		list_pool.add(thread);
	    }
	}

	if (thread == null) {
	    // None available.  This is unrecoverable.
	    throw new RuntimeException("Exceeded ThreadPool max");
	}

	thread.setRunnable(runnable);
	if (logger.isInfoEnabled()) thread.setName(name);

	return thread;
    }
  
    /** actually construct a new PooledThread **/
    PooledThread constructReusableThread() {
	// If info logging is enabled the thread's name will get set
	// when it's run, so it can start out empty.
	String name = "";
	if (!logger.isInfoEnabled()) name = nextName();
	return new PooledThread(this, name);
    }


    String generateName() {
	// Generate a name for a Schedulable.  If info logging is
	// enabled the name won't be used anywhere, so just return
	// null.  Otherwise make a unique one.
	if (logger.isInfoEnabled()) 
	    return nextName();
	else
	    return null;
    }

    void listActiveThreads(List records) {
	PooledThread thread = null;
	long now = System.currentTimeMillis();
	long elapsed = 0;
	for (int i=0; i<pool.length; i++) {
	    thread = pool[i];
	    if (thread != null && thread.isRunning) {
		ThreadStatusService.Record record = 
		    new ThreadStatusService.ActiveRecord();
		try {
		    SchedulableObject sched = thread.schedulable;
		    Object consumer = sched.getConsumer();
		    if (consumer != null)
			record.consumer = consumer.toString();
		    Scheduler scheduler = sched.getScheduler();
		    if (scheduler != null)
			record.scheduler = scheduler.getName();
		    record.schedulable = sched.getName();
		    record.elapsed = now-thread.start_time;
		    records.add(record);
		} catch (Throwable t) {
		}
	    }
	}
    }
}
