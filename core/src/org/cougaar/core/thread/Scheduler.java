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

import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.PropertyParser;

import java.util.Comparator;


public class Scheduler 
    implements ThreadControlService
{
    private static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    
    // O means unlimited
    private static final int MaxRunningCountDefault = 10;

    private DynamicSortedQueue pendingThreads;
    private ThreadListenerProxy listenerProxy;
    private String name;
    private String printName;
    private TreeNode treeNode;
    private int maxRunningThreads;
    private int runningThreadCount = 0;

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






    synchronized void addPendingThread(SchedulableObject thread) 
    {
	if (pendingThreads.contains(thread)) return;
	thread.notifyPending();
	listenerProxy.notifyQueued(thread);
	pendingThreads.add(thread);
    }

    synchronized void dequeue(SchedulableObject thread) 
    {
	pendingThreads.remove(thread);
	threadDequeued(thread);
    }



    void threadDequeued(SchedulableObject thread) {
	if (CougaarThread.Debug)
	    System.out.println(" Dequeued " +thread+
			       " run count=" +runningThreadCount+
			       " queue count=" +pendingThreads.size());
	listenerProxy.notifyDequeued(thread);
    }

    // Called within the thread itself as the first thing it does.
    void threadClaimed(SchedulableObject thread) {
	if (CougaarThread.Debug)
	    System.out.println(" Started " +thread+
			       " run count=" +runningThreadCount+
			       " queue count=" +pendingThreads.size());
	listenerProxy.notifyStart(thread);
    }

    // Called within the thread itself as the last thing it does.
    void threadReclaimed(SchedulableObject thread) {
	if (CougaarThread.Debug)
	    System.out.println(" Ended " +thread+ 
			       " run count=" +runningThreadCount+
			       " queue count=" +pendingThreads.size());
	listenerProxy.notifyEnd(thread);
    }


    // Suspend/Resume "hints" -- not used yet.
    void threadResumed(SchedulableObject thread) {
	if (CougaarThread.Debug)
	    System.out.println(" Resumed " +thread+
			       " run count=" +runningThreadCount+
			       " queue count=" +pendingThreads.size());
    }

    void threadSuspended(SchedulableObject thread) {
	if (CougaarThread.Debug)
	    System.out.println(" Suspended " +thread+
			       " run count=" +runningThreadCount+
			       " queue count=" +pendingThreads.size());
    }



    synchronized void incrementRunCount(Scheduler consumer) {
	++runningThreadCount;
	listenerProxy.notifyRightGiven(consumer);
    }

    synchronized void decrementRunCount(Scheduler consumer) {
	--runningThreadCount;
	if (runningThreadCount < 0) {
	    System.err.println("Thread account oddity: " +this+ 
			       " thread count is " +
			       runningThreadCount);
	}
	listenerProxy.notifyRightReturned(consumer);
    }



    SchedulableObject getNextPending() {
	return popQueue();
    }

    SchedulableObject popQueue() {
	SchedulableObject thread = null;
	synchronized(this) {
	    thread = (SchedulableObject)pendingThreads.next();
	    if (thread != null) incrementRunCount(this);
	}

	// Notify listeners
	if (thread != null) threadDequeued(thread);

	return thread;
    }


    synchronized boolean requestRights(Scheduler requestor) {
	if (maxRunningThreads < 0 || runningThreadCount < maxRunningThreads) {
	    incrementRunCount(requestor);
	    return true;
	}
	return false;
    }

    synchronized void releaseRights(Scheduler consumer) {
	// If the max has recently decreased it may be lower than the
	// running count.  In that case don't do a handoff.
	decrementRunCount(consumer);
	SchedulableObject handoff = null;

	if (runningThreadCount <= maxRunningThreads) {
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
