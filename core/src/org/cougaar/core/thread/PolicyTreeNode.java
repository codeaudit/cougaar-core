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

final class PolicyTreeNode
{
    private TimeSlicePolicy policy;
    private PolicyTreeNode parent;
    private ArrayList children;
    private Scheduler scheduler;

    PolicyTreeNode(TimeSlicePolicy policy, 
		   Scheduler scheduler,
		   PolicyTreeNode parent) 
    {
	children = new ArrayList();
	this.scheduler = scheduler;
	scheduler.setTreeNode(this);
	setPolicy(policy);
	setParent(parent);
    }


    void setParent(PolicyTreeNode parent) {
	this.parent = parent;
	if (parent != null) parent.addChild(this);
    }

    PolicyTreeNode getParent() {
	return parent;
    }

    TimeSlicePolicy getParentPolicy() {
	if (parent == null)
	    return null;
	else
	    return parent.getPolicy();
    }

    void addChild(PolicyTreeNode child) {
	children.add(child);
    }

    ArrayList getChildren() {
	return children;
    }


    void setPolicy(TimeSlicePolicy policy) {
	this.policy = policy;
	policy.setTreeNode(this);
    }

    TimeSlicePolicy getPolicy() {
	return policy;
    }

    Scheduler getScheduler() {
	return scheduler;
    }

    String getName() {
	return scheduler.name;
    }
}
