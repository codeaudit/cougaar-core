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


public class PropagatingScheduler extends Scheduler
{
    private RightsSelector selector;

    public PropagatingScheduler(ThreadListenerProxy listenerProxy, 
				String name)
    {
	super(listenerProxy, name);

	// Default selector
	selector = new RoundRobinSelector();
	selector.setScheduler(this);
    }

    
    public void setRightsSelector(RightsSelector selector) {
	this.selector = selector;
	selector.setScheduler(this);
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
	    if (ok) incrementRunCount(this);
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
	    decrementRunCount(this);
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


    // Hack so the selector can get at the super method
    SchedulableObject getNextPendingSuper() {
	return super.getNextPending();
    }

    SchedulableObject getNextPending() {
	return selector.getNextPending();
    }

}
