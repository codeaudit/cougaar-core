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

public class RightsPropagatingScheduler extends Scheduler
{
    private static final long MaxTime = 20; // ms
    private int ownedRights = 0;
    private long lastReleaseTime = 0;

    public RightsPropagatingScheduler(ThreadListenerProxy listenerProxy, 
				      String name)
    {
	super(listenerProxy, name);
	System.out.println("RightsPropagatingScheduler");
    }

    
    boolean requestRights(Scheduler requestor) {
	TreeNode parent_node = getTreeNode().getParent();
	boolean result;
	if (parent_node == null) {
	    // This is the root
	    result = super.requestRights(requestor);
	} else if (ownedRights > 0) {
	    return false;
	} else {
	    Scheduler parent = parent_node.getScheduler();
	    result = parent.requestRights(this);
	}
	synchronized (this) { if (result) ++ownedRights; }
	return result;
    }

    
    void releaseRights(Scheduler consumer, SchedulableObject thread) { 
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    super.releaseRights(consumer, thread);
	} else {
	    long now = System.currentTimeMillis();
	    if (now - lastReleaseTime > MaxTime) {
		releaseToParent(consumer, thread);
	    } else {
		offerRights(consumer);
	    }
	}
   }

    private void releaseToParent(Scheduler consumer, SchedulableObject thread) {
	TreeNode parent_node = getTreeNode().getParent();
	Scheduler parent = parent_node.getScheduler();
	parent.releaseRights(this, thread);
	lastReleaseTime = System.currentTimeMillis();
	synchronized (this) { --ownedRights; }
    }

    private synchronized void offerRights(Scheduler consumer) {
	SchedulableObject handoff = getNextPending();
	if (handoff != null) {
	    handoff.thread_start();
	} else {
	    releaseToParent(consumer, handoff);
	}
    }




    // Holds the next index of the round-robin selection.  A value of
    // -1 refers to the local queue, a value >= 0 refers to the
    // corresponding child.
    private int currentIndex = -1;

    private SchedulableObject checkNextPending(ArrayList children) {
	SchedulableObject handoff = null;
	if (currentIndex == -1) {
	    handoff = super.getNextPending();
	    currentIndex = children.size() == 0 ? -1 : 0;
	} else {
	    TreeNode child_node =(TreeNode) children.get(currentIndex++);
	    if (currentIndex == children.size()) currentIndex = -1;
	    Scheduler child = child_node.getScheduler();
	    handoff = child.getNextPending();
	}
	return handoff;
    }

    SchedulableObject getNextPending() {
	int initialIndex = currentIndex;
	ArrayList children = getTreeNode().getChildren();
	SchedulableObject handoff = null;
	// repeat-until
	while (true) {
	    handoff = checkNextPending(children);
	    if (handoff != null) return handoff;
	    if (currentIndex == initialIndex) break;
	}
	return null;
    }

}
