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

public class SimplePropagatingScheduler extends Scheduler
{
    public SimplePropagatingScheduler(ThreadListenerProxy listenerProxy, 
				      String name)
    {
	super(listenerProxy, name);
    }

    
    boolean requestRights(Scheduler requestor) {
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    return super.requestRights(requestor);
	} else {
	    Scheduler parent = parent_node.getScheduler();
	    boolean ok = parent.requestRights(this);
	    // If our parent gave us a right, increase our local count
	    if (ok) ++runningThreadCount;
	    return ok;
	}
    }

    
    void releaseRights(Scheduler consumer) { 
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    super.releaseRights(consumer);
	} else {
	    Scheduler parent = parent_node.getScheduler();
	    parent.releaseRights(this);
	    // In this simple scheduler, layers other than root always
	    // give up the right at this point (root may hand it off).
	    --runningThreadCount;
	}
   }


    // The maxRunningThreads instance variable is irrelevant except at
    // the root scheduler.
    public void setMaxRunningThreadCount(int count) {
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    super.setMaxRunningThreadCount(count);
	} else {
	    Scheduler parent = parent_node.getScheduler();
	    parent.setMaxRunningThreadCount(count);
	}
    }

    public int maxRunningThreadCount() {
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // This is the root
	    return super.maxRunningThreadCount();
	} else {
	    Scheduler parent = parent_node.getScheduler();
	    return parent.maxRunningThreadCount();
	}
    }


    // Holds the next index of the round-robin selection.  A value of
    // -1 refers to the local queue, rather than any of the children.
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
	    // We're the parent of the Scheduler to which the handoff
	    // is given.  Increase the local count.
	    if (handoff != null) ++runningThreadCount;
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
