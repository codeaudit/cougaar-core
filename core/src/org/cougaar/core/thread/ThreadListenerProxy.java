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

import org.cougaar.core.service.ThreadListenerService;

/**
 * Proxy for ThreadListenerService
 */
final class ThreadListenerProxy implements ThreadListenerService
{
    private ArrayList[] listenersList;
    private TreeNode node;

    ThreadListenerProxy(int laneCount) 
    {
	listenersList = new ArrayList[laneCount];
	for (int i=0; i<listenersList.length; i++)
	    listenersList[i] = new ArrayList();
    }
		    
    void setTreeNode(TreeNode node)
    {
	this.node = node;
    }

    ArrayList getListeners(SchedulableObject schedulable) 
    {
	return listenersList[schedulable.getLane()];
    }

    ArrayList getListeners(Scheduler scheduler) 
    {
	return listenersList[scheduler.getLane()];
    }

    synchronized void notifyQueued(SchedulableObject schedulable) 
    {
	Object consumer = schedulable.getConsumer();
	ArrayList listeners = getListeners(schedulable);
        for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadQueued(schedulable, consumer);
	}
    }

    synchronized void notifyDequeued(SchedulableObject schedulable) {
	Object consumer = schedulable.getConsumer();
	ArrayList listeners = getListeners(schedulable);
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadDequeued(schedulable, consumer);
	}
    }

    synchronized void notifyStart(SchedulableObject schedulable) 
    {
	Object consumer = schedulable.getConsumer();
	ArrayList listeners = getListeners(schedulable);
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadStarted(schedulable, consumer);
	}
    }

    synchronized void notifyEnd(SchedulableObject schedulable) 
    {
	Object consumer = schedulable.getConsumer();
	ArrayList listeners = getListeners(schedulable);
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.threadStopped(schedulable, consumer);
	}
    }

    synchronized void notifyRightGiven(Scheduler scheduler) 
    {
	String id = scheduler.getName();
	ArrayList listeners = getListeners(scheduler);
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.rightGiven(id);
	}
    }

    synchronized void notifyRightReturned(Scheduler scheduler) 
    {
	String id = scheduler.getName();
	ArrayList listeners = getListeners(scheduler);
	for (int i = 0, n = listeners.size(); i < n; i++) {
	    ThreadListener listener = (ThreadListener) listeners.get(i);
	    listener.rightReturned(id);
	}
    }




    public synchronized void addListener(ThreadListener listener,
					 int lane) 
    {
	if (lane < 0 || lane >= listenersList.length)
	    throw new RuntimeException("Lane is out of range: " +lane);
	listenersList[lane].add(listener);
    }


    public synchronized void removeListener(ThreadListener listener,
					    int lane) 
    {
	if (lane < 0 || lane >= listenersList.length)
	    throw new RuntimeException("Lane is out of range: " +lane);
	listenersList[lane].remove(listener);
    }


    public synchronized void addListener(ThreadListener listener) 
    {
	listenersList[node.getDefaultLane()].add(listener);
    }


    public synchronized void removeListener(ThreadListener listener) 
    {
	listenersList[node.getDefaultLane()].remove(listener);
    }

}
