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


abstract class Scheduler 
    implements ThreadControlService, TimeSliceConsumer
{
    private static final String MaxRunningCountProp =
	"org.cougaar.thread.running.max";
    
    // O means unlimited
    private static final int MaxRunningCountDefault = 10;

    private DynamicSortedQueue pendingThreads;
    private int maxRunningThreads;
    private int runningThreadCount = 0;
    private ThreadListenerProxy listenerProxy;
    private String name;
    private String printName;
    private TreeNode treeNode;

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



    Scheduler(ThreadListenerProxy listenerProxy, String  name) {
	pendingThreads = new DynamicSortedQueue(timeComparator);
	maxRunningThreads = 
	    PropertyParser.getInt(MaxRunningCountProp, 
				  MaxRunningCountDefault);
	this.listenerProxy = listenerProxy;
	this.name = name;
	printName = "<Scheduler " +name+ ">";
    }

    public String toString() {
	return printName;
    }


    void setTreeNode(TreeNode treeNode) {
	this.treeNode = treeNode;
    }


    // TimeSliceConsumer
    public String getName() {
	return name;
    }


    // ThreadControlService 
    public synchronized void setQueueComparator(Comparator comparator)
    {
	pendingThreads.setComparator(comparator);
    }

    public synchronized void setTimeSlicePolicy(TimeSlicePolicy policy)
    {
	treeNode.setPolicy(policy);
    }

    public synchronized void setMaxRunningThreadCount(int count) {
	maxRunningThreads = count;
	treeNode.getPolicy().setMaxRunningThreadCount(count);
	changedMaxRunningThreadCount();
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



    public boolean offerSlice(TimeSlice slice) {
	return false;
    }


    synchronized SchedulableObject nextPendingThread() {
	return (SchedulableObject)pendingThreads.next();
    }


    synchronized SchedulableObject nextPendingThread(SchedulableObject thrd)
    {
	return (SchedulableObject)pendingThreads.next(thrd);
    }

    TimeSlicePolicy getPolicy() {
	return treeNode.getPolicy();
    }

    TimeSlice getSlice() {
	return getPolicy().getSlice(this);
    }

    void releaseSlice(TimeSlice slice) {
	getPolicy().releaseSlice(this, slice);
    }

    void noteChangeOfOwnership(TimeSlice slice) {
	getPolicy().noteChangeOfOwnership(this, slice);
    }

    synchronized void addPendingThread(SchedulableObject thread) 
    {
	if (pendingThreads.contains(thread)) return;
	thread.notifyPending();
	listenerProxy.notifyPending(thread);
	pendingThreads.add(thread);
    }

    synchronized void dequeue(SchedulableObject thread) 
    {
	pendingThreads.remove(thread);
    }




    // Called when a request has been made to start the thread (from
    // some other thread).  The count needs to be adjusted here, not
    // when the thread actually starts running.
    void threadStarting(SchedulableObject thread) {
	synchronized (this) { ++runningThreadCount; }
	if (CougaarThread.Debug)
	    System.out.println("Started " +thread+
			       ", count=" +runningThreadCount);
    }

    // Called within the thread itself as the first thing it does.
    void threadClaimed(SchedulableObject thread) {
	listenerProxy.notifyStart(thread);
    }

    // Called within the thread itself as the last thing it does.
    void threadReclaimed(SchedulableObject thread) {
	synchronized (this) { --runningThreadCount; }
	if (CougaarThread.Debug)
	    System.out.println("Ended " +thread+ 
			       ", count=" +runningThreadCount);
	listenerProxy.notifyEnd(thread);
    }

    void threadResumed(SchedulableObject thread) {
	synchronized (this) { ++runningThreadCount; }
	if (CougaarThread.Debug)
	    System.out.println("Resumed " +thread+
			       ", count=" +runningThreadCount);
    }

    void threadSuspended(SchedulableObject thread) {
	synchronized (this) { --runningThreadCount; }
	if (CougaarThread.Debug)
	    System.out.println("Suspended " +thread+
			       ", count=" +runningThreadCount);
    }


    // Called when a thread is about to suspend.
    synchronized void suspendThread(SchedulableObject thread) {
	threadSuspended(thread);
    }


    // Called when a thread is about to resume
    synchronized void resumeThread(SchedulableObject thread) {
	threadResumed(thread);
    }




    abstract void changedMaxRunningThreadCount();

    // Yield only if there's a candidate to yield to.  Called when
    // a thread wants to yield (as opposed to suspend).
    abstract boolean maybeYieldThread(SchedulableObject thread);

    // Try to resume a suspended or yielded thread, queuing
    // otherwise.
    abstract boolean maybeResumeThread(SchedulableObject thread);

    abstract void startOrQueue(SchedulableObject thread);

}
