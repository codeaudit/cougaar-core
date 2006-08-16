/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.thread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

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
 */
public class Scheduler 
{
    private DynamicSortedQueue pendingThreads;
    private ArrayList disqualified = new ArrayList();
    private UnaryPredicate qualifier;
    private UnaryPredicate childQualifier;
    private ThreadListenerProxy listenerProxy;
    private String printName;
    private TreeNode treeNode;
    private int absoluteMax;
    private int maxRunningThreads=0;
    private int runningThreadCount = 0;
    private int lane;
    protected Logger logger;
    protected int rightsRequestCount = 0;

    private static Logger _logger = Logging.getLogger(Scheduler.class);

    private Comparator timeComparator =
	new Comparator() {
		public boolean equals(Object x) {
		    return x == this;
		}

		public int compare (Object x, Object y) {
		    long t1 = ((SchedulableObject) x).getTimestamp();
		    long t2 = ((SchedulableObject) y).getTimestamp();
		    if (t1 < t2)
			return -1;
		    else if (t1 > t2)
			return 1;
		    else
			return 0;
		}
	    };



    public Scheduler(ThreadListenerProxy listenerProxy) 
    {
	pendingThreads = new DynamicSortedQueue(timeComparator);
	this.listenerProxy = listenerProxy;
    }

    void setAbsoluteMax(int absoluteMax) 
    {
	this.absoluteMax = absoluteMax;
	maxRunningThreads = absoluteMax;
        if (_logger.isInfoEnabled()) {
          _logger.info("Initialized maxRunningThreads to "+maxRunningThreads);
        }
    }

    // NB: By design this method is NOT synchronized! It should only
    // be used by the ThreadStatusService and is intended to provide
    // a best-effort snapshot.  Failures are normal. 
    int iterateOverQueuedThreads(ThreadStatusService.Body body)
    {
	// could copy the queue in a synchronized block, on the off
	// chance that read-access is unsafe otherwise.  But there are
	// no indications this is really an issue.
	try {
	    return pendingThreads.processEach(body, getName(), _logger);
	} catch (IndexOutOfBoundsException r) {
          // this may happen if pendingThreads was modified during the traversal
          if (_logger.isDebugEnabled()) {
            _logger.debug("SortedQueue.processEach detected a collision", r);
          }
        } catch (Throwable r) {
            _logger.error("SortedQueue.processEach threw an uncaught exception", r);
	}
        return 0;
    }


    private synchronized Logger getLogger() {
	if (logger == null) logger = Logging.getLogger(getClass().getName());
	return logger;
    }

    public void setRightsSelector(RightsSelector selector) {
	// error? no-op?
    }

    public String toString() {
	return printName;
    }


    void setTreeNode(TreeNode treeNode) {
	this.treeNode = treeNode;
	printName = "<Scheduler " +treeNode.getName()+ ">";
    }


    TreeNode getTreeNode() {
	return treeNode;
    }

    int getLane() {
	return lane;
    }

    void setLane(int lane) {
	this.lane = lane;
    }

    String getName() {
	return getTreeNode().getName();
    }

    // ThreadControlService 
    public synchronized void setQueueComparator(Comparator comparator)
    {
	if (comparator != null) 
	    pendingThreads.setComparator(comparator);
	else
	    pendingThreads.setComparator(timeComparator);
    }


    public int maxRunningThreadCount() {
	return maxRunningThreads;
    }

    public int pendingThreadCount() {
	return pendingThreads.size();
    }

    public int runningThreadCount() {
	return runningThreadCount;
    }

    // synchronize to keep the two addends consistent
    public synchronized int activeThreadCount() {
	return runningThreadCount + pendingThreads.size();
    }




    public boolean setChildQualifier(UnaryPredicate predicate) {
	if (predicate == null) {
	    childQualifier = null;
	    return true;
	} else if (childQualifier == null) {
	    childQualifier = predicate;
	    return true;
	} else {
	    // log an error
	    Logger logger = getLogger();
	    if (logger.isErrorEnabled())
		logger.error("ChildQualifier is already set");
	    return false;
	}
    }


    boolean allowRightFor(Scheduler child) {
	if (child == this || childQualifier == null) {
	    // Don't run this on yourself, leave it to the parent.
	    return true;
	} else {
	    return childQualifier.execute(child);
	}
	

    }



    void addPendingThread(SchedulableObject thread) 
    {
	synchronized (this) {
	    if (pendingThreads.contains(thread)) return;
	    thread.setQueued(true);
	    pendingThreads.add(thread);
	}
	listenerProxy.notifyQueued(thread);
    }

    synchronized void dequeue(SchedulableObject thread) 
    {
	pendingThreads.remove(thread);
	threadDequeued(thread);
    }


    void threadDequeued(SchedulableObject thread) {
	listenerProxy.notifyDequeued(thread);
    }

    // Called within the thread itself as the first thing it does.
    void threadClaimed(SchedulableObject thread) {
	listenerProxy.notifyStart(thread);
    }

    // Called within the thread itself as the last thing it does.
    SchedulableObject threadReclaimed(SchedulableObject thread,
				      boolean reuse)
    {
	listenerProxy.notifyEnd(thread);
	if (reuse) 
	    synchronized(this) {
		return (SchedulableObject) pendingThreads.next();
	    }
	else
	    return null;
    }


    // Suspend/Resume "hints" -- not used yet.
    void threadResumed(SchedulableObject thread) {
    }

    void threadSuspended(SchedulableObject thread) {
    }



    void incrementRunCount(Scheduler consumer) {
	synchronized (this) {
	    ++runningThreadCount;
	    listenerProxy.notifyRightGiven(consumer);
	}
    }

    void decrementRunCount(Scheduler consumer) {
	synchronized (this) {
	    --runningThreadCount;
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

    synchronized void releaseRights(Scheduler consumer) {
	// If the max has recently decreased it may be lower than the
	// running count.  In that case don't do a handoff.
	decrementRunCount(consumer);
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

    public void setMaxRunningThreadCount(int requested_max) {
	int count = requested_max;
        Logger logger = getLogger();
	if (requested_max > absoluteMax) {
	    if (logger.isErrorEnabled())
		logger.error("Attempt to set maxRunningThreadCount to "
			     +requested_max+
			     " which is greater than the absolute max of "
			     +absoluteMax);
	    count = absoluteMax;
	} else {
          if (_logger.isInfoEnabled()) {
            _logger.info("Setting maxRunningThreadCount to "+requested_max+" from "+maxRunningThreads,
                        new Throwable());
          }
        }
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




    private boolean qualified(Schedulable thread) {
	return qualifier == null || qualifier.execute(thread);
    }

    public boolean setQualifier(UnaryPredicate predicate) {
	if (predicate == null) {
            List requeue;
	    synchronized (this) {
                qualifier = null;
                requeue = new ArrayList(disqualified);
                disqualified.clear();
                // start-or-queue any previosly disqualified items
            }
	    Logger logger = getLogger();
	    if (logger.isDebugEnabled())
		logger.debug("Restoring " + requeue.size() + 
			     " previously disqualified threads");
            for (int i = 0, n = requeue.size(); i < n; i++) {
                SchedulableObject sched = (SchedulableObject) requeue.get(i);
                Starter.push(sched);
            }
            return true;
	} else if (qualifier == null) {
	    synchronized (this) {
                qualifier = predicate;
                List bad = pendingThreads.filter(predicate);
                // move any disqualified items on the queue to the
                // disqualified list
                boolean useItr = (bad instanceof RandomAccess); 
                Iterator itr = (useItr ? bad.iterator() : null);
                for (int i = 0, n = bad.size(); i < n; i++) {
                    SchedulableObject thread = (SchedulableObject)
                      (useItr ? itr.next() : bad.get(i));
                    disqualify(thread);
                }
                return true;
            }
	} else {
	    Logger logger = getLogger();
	    if (logger.isErrorEnabled())
		logger.error("Qualifier is already set");
	    return false;
	}
    }


    private void disqualify(SchedulableObject sched) {
	sched.setDisqualified(true);
	if (!disqualified.contains(sched)) disqualified.add(sched);
    }

	    

    void startOrQueue(SchedulableObject thread) {
	// If the queue isn't empty, queue this one too.
	synchronized (this) {
	    if (!qualified(thread)) {
		disqualify(thread);
		return;
	    }
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
