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

import org.cougaar.core.qos.metrics.MetricsServiceProvider;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.PropertyParser;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Comparator;


/**
 * The base class of thread-scheduler.  It allows a certain maximum
 * number of threads to running, queuing any requests beyond that.  An
 * items is dequeued when a running thread stops.  The maximum is per
 * thread-service, not global.  This scheduler is not used by default
 * (the PropagatingScheduler extension is the default).
 *
 * @property org.cougaar.thread.running.max specifies the maximum
 * number of running threads.  A negative number is interpreted to
 * mean that there is no maximum.  The precise meaning of 'maximum' is
 * varies by scheduler class.
 *
 * @property org.cougaar.thread.logfile If set, extensive logging
 * information will be saved in the given file, in tab-separated
 * format (readable by Exel).
 */
public class Scheduler 
    implements ThreadControlService
{
    private static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    
    // O means unlimited
    private static final int MaxRunningCountDefault = 30;

    private DynamicSortedQueue pendingThreads;
    private ThreadListenerProxy listenerProxy;
    private String name;
    private String printName;
    private TreeNode treeNode;
    private int maxRunningThreads;
    private int runningThreadCount = 0;
    protected int rightsRequestCount = 0;

    private Comparator timeComparator =
	new Comparator() {
		public boolean equals(Object x) {
		    return x == this;
		}

		public int compare (Object x, Object y) {
		    long t1 = ((SchedulableObject) x).timestamp();
		    long t2 = ((SchedulableObject) y).timestamp();
		    if (t1 < t2)
			return -1;
		    else if (t1 > t2)
			return 1;
		    else
			return 0;
		}
	    };



    public Scheduler(ThreadListenerProxy listenerProxy, String  name) {
	pendingThreads = new DynamicSortedQueue(timeComparator);
	maxRunningThreads = 
	    PropertyParser.getInt(MaxRunningCountProp, 
				  MaxRunningCountDefault);
	this.listenerProxy = listenerProxy;
	this.name = name;
	printName = "<Scheduler " +name+ ">";
    }


    public void setRightsSelector(RightsSelector selector) {
	// error? no-op?
    }

    public String toString() {
	return printName;
    }


    void setTreeNode(TreeNode treeNode) {
	this.treeNode = treeNode;
    }


    TreeNode getTreeNode() {
	return treeNode;
    }


    String getName() {
	return name;
    }

    // ThreadControlService 
    public synchronized void setQueueComparator(Comparator comparator)
    {
	pendingThreads.setComparator(comparator);
    }


    public int maxRunningThreadCount() {
	return maxRunningThreads;
    }

    public synchronized int pendingThreadCount() {
	return pendingThreads.size();
    }

    public synchronized int runningThreadCount() {
	return runningThreadCount;
    }


    public synchronized int activeThreadCount() {
	return runningThreadCount + pendingThreads.size();
    }






    void addPendingThread(SchedulableObject thread) 
    {
	synchronized (this) {
	    if (pendingThreads.contains(thread)) return;
	    thread.notifyPending();
	    pendingThreads.add(thread);
	}
	listenerProxy.notifyQueued(thread);
    }

    synchronized void dequeue(SchedulableObject thread) 
    {
	pendingThreads.remove(thread);
	threadDequeued(thread);
    }


    private static PrintWriter log;
    private static final String LOGFILE_PROPERTY =
	"org.cougaar.thread.logfile";

    private synchronized static boolean ensureLog() {
	if (log == null) {
	    String filename = System.getProperty(LOGFILE_PROPERTY);
	    if (filename == null) return false;
	    try {
		FileWriter fw = new FileWriter(filename);
		log = new PrintWriter(fw);
	    } catch (java.io.IOException ex) {
		System.err.println(ex);
	    }
	}
	return log != null;
    }

    private void log(String action, Object item) {
	if (!ensureLog()) return;
	long now = MetricsServiceProvider.relativeTimeMillis();
	StringBuffer buf = new StringBuffer();
	buf.append(Double.toString(now/1000.0));
	buf.append('\t');
	buf.append(action);
	buf.append('\t');
	buf.append(Integer.toString(runningThreadCount));
	buf.append('\t');
	buf.append(this.toString());
	buf.append('\t');
	if (item != null) buf.append(item.toString());
	log.println(buf.toString());
    }

    void threadDequeued(SchedulableObject thread) {
	log("dequeued", thread);
	listenerProxy.notifyDequeued(thread);
    }

    // Called within the thread itself as the first thing it does.
    void threadClaimed(SchedulableObject thread) {
	log("claimed", thread);
	listenerProxy.notifyStart(thread);
    }

    // Called within the thread itself as the last thing it does.
    void threadReclaimed(SchedulableObject thread) {
	log("reclaimed", thread);
	listenerProxy.notifyEnd(thread);
    }


    // Suspend/Resume "hints" -- not used yet.
    void threadResumed(SchedulableObject thread) {
    }

    void threadSuspended(SchedulableObject thread) {
    }



    void incrementRunCount(Scheduler consumer) {
	synchronized (this) {
	    ++runningThreadCount;
	    log("increment", consumer);
	    listenerProxy.notifyRightGiven(consumer);
	}
    }

    void decrementRunCount(Scheduler consumer, SchedulableObject thread) {
	synchronized (this) {
	    --runningThreadCount;
	    log("decrement", (consumer == this ? ((Object) thread) : ((Object) consumer)));
	    if (runningThreadCount < 0) {
		System.out.println("###" +this+ " thread count is " +
				   runningThreadCount);
	    }
	    listenerProxy.notifyRightReturned(consumer);
	}
    }



    SchedulableObject getNextPending() {
	return popQueue();
    }

    SchedulableObject popQueue() {
	SchedulableObject thread = null;
	synchronized(this) {
	    if (!checkLocalRights()) return null;
	    thread = (SchedulableObject)pendingThreads.next();
	    if (thread != null) incrementRunCount(this);
	}

	// Notify listeners
	if (thread != null) threadDequeued(thread);

	return thread;
    }

    // Caller should synchronize.  
    boolean checkLocalRights () {
	if (maxRunningThreads < 0) return true;
	return runningThreadCount+rightsRequestCount < maxRunningThreads;
    }


    synchronized boolean requestRights(Scheduler requestor) {
	if (maxRunningThreads < 0 || runningThreadCount < maxRunningThreads) {
	    incrementRunCount(requestor);
	    return true;
	}
	return false;
    }

    synchronized void releaseRights(Scheduler consumer, SchedulableObject thread) {
	// If the max has recently decreased it may be lower than the
	// running count.  In that case don't do a handoff.
	decrementRunCount(consumer, thread);
	SchedulableObject handoff = null;

	if (runningThreadCount < maxRunningThreads) {
	    handoff = getNextPending();
	    if (handoff != null) handoff.thread_start();
	} else {
// 	    System.err.println("Decreased thread count prevented handoff " 
// 			       +runningThreadCount+ ">" 
// 			       +maxRunningThreads);
	}
    }

    public void setMaxRunningThreadCount(int count) {
	int additionalThreads = count - maxRunningThreads;
	maxRunningThreads = count;
 	TreeNode parent_node = getTreeNode().getParent();
 	if (parent_node != null) return;
	
	// If we get here, we're the root node.  Try to run more
	// threads if the count has gone up.
	for (int i=0; i<additionalThreads; i++) {
	    SchedulableObject schedulable = null;
	    synchronized (this) {
		schedulable = getNextPending();
	    }
	    if (schedulable == null) return;
// 	    System.err.println("Increased thread count let me start one!");
	    schedulable.thread_start();
	}
    }

    void startOrQueue(SchedulableObject thread) {
	// If the queue isn't empty, queue this one too.
	synchronized (this) {
	    if (pendingThreadCount() > 0) {
		addPendingThread(thread);
		return;
	    }
	}

	boolean can_run = requestRights(this);
	if (can_run) {
	    thread.thread_start();
	} else {
	    addPendingThread(thread);
	}
    }
    

}
