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


class SimplePropagatingScheduler extends SimpleScheduler
{
    SimplePropagatingScheduler(ThreadListenerProxy listenerProxy, String name)
    {
	super(listenerProxy, name);
    }

    
    boolean requestRights(SimpleScheduler requestor) {
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    return super.requestRights(requestor);
	} else {
	    SimpleScheduler parent = (SimpleScheduler) 
		parent_node.getScheduler();
	    return parent.requestRights(this);
	}
    }

    
    void releaseRights(SimpleScheduler consumer) { 
	TreeNode parent_node = getTreeNode().getParent();
	if (parent_node == null) {
	    // Here we need to give other children a chance to claim
	    // the newly available slot
	    super.releaseRights(consumer);
	} else {
	    SimpleScheduler parent = (SimpleScheduler) 
		parent_node.getScheduler();
	    parent.releaseRights(this);
	}
   }


    SchedulableObject getNextPending() {
	SchedulableObject handoff = super.getNextPending();
	if (handoff != null) return handoff;

	java.util.Iterator itr = getTreeNode().getChildren().iterator();
	while (itr.hasNext()) {
	    TreeNode child_node = (TreeNode) itr.next();
	    SimpleScheduler child = (SimpleScheduler) 
		child_node.getScheduler();
	    handoff = child.getNextPending();
	    if (handoff != null) return handoff;
	}
	return null;
    }

}
