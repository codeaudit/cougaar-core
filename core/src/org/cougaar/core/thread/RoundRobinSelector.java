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

class RoundRobinSelector implements RightsSelector
{
    // Holds the next index of the round-robin selection.  A value of
    // -1 refers to the local queue, rather than any of the children.
    private int currentIndex = -1;
    protected PropagatingScheduler scheduler;

    public void setScheduler(PropagatingScheduler scheduler) {
	this.scheduler = scheduler;
    }

    private SchedulableObject checkNextPending(ArrayList children) 
    {
	SchedulableObject handoff = null;
	int child_count = children.size();
	if (currentIndex == -1) {
	    handoff = scheduler.popQueue();
	    currentIndex = child_count == 0 ? -1 : 0;
	} else {
	    TreeNode child_node =(TreeNode) children.get(currentIndex++);
	    if (currentIndex == child_count) currentIndex = -1;
	    Scheduler child = child_node.getScheduler();
	    handoff = child.getNextPending();
	    // We're the parent of the Scheduler to which the handoff
	    // is given.  Increase the local count.
	    if (handoff != null) scheduler.incrementRunCount(child);
	}
	return handoff;
    }

    public SchedulableObject getNextPending() {
	int initialIndex = currentIndex;
	ArrayList children = scheduler.getTreeNode().getChildren();
	SchedulableObject handoff = null;
	do {
	    handoff = checkNextPending(children);
	    if (handoff != null) return handoff;
	} while (currentIndex != initialIndex);
	return null;
    }
}
