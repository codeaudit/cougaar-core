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
import java.util.List;
import java.util.Timer;

import org.cougaar.core.service.ThreadService;

final class TreeNode
{
    private TreeNode parent;
    private ArrayList children;
    private Scheduler[] schedulers;
    private ThreadPool[] pools;
    private Timer timer;
    private String name;
    private int defaultLane = ThreadService.BEST_EFFORT_LANE; // parameter?

    TreeNode(Scheduler[] schedulers, 
	     ThreadPool[] pools,
	     String name,
	     ThreadServiceProxy parentService) 
    {
	this.name = name;
	children = new ArrayList();
	this.schedulers = schedulers;
	this.pools = pools;
	for (int i=0; i<schedulers.length; i++) schedulers[i].setTreeNode(this);
	TreeNode parent = 
	    parentService == null ? null : parentService.getTreeNode();
	setParent(parent);
    }

    int getDefaultLane() 
    {
	return defaultLane;
    }

    void setDefaultLane(int lane)
    {
	defaultLane = lane;
    }

    int getLaneCount() 
    {
	return schedulers.length;
    }

    synchronized Timer timer() 
    {
	if (timer == null) timer = new Timer(true);
	return timer;
    }


    void setParent(TreeNode parent)
    {
	this.parent = parent;
	if (parent != null) parent.addChild(this);
    }

    TreeNode getParent() 
    {
	return parent;
    }


    void addChild(TreeNode child) 
    {
	synchronized (children) {
	    children.add(child);
	}
    }

    ArrayList getChildren() 
    {
	return children;
    }

    void listRunningThreads(List records) 
    {
	// All pools
	for (int i=0; i<pools.length; i++)
	    pools[i].listRunningThreads(records);
    }

    void listQueuedThreads(List records) 
    {
	for (int i=0; i<schedulers.length; i++)
	    schedulers[i].listQueuedThreads(records);
	if (children != null) {
	    synchronized (children) {
		for (int i = 0, n = children.size(); i < n; i++) {
		    TreeNode child = (TreeNode) children.get(i);
		    child.listQueuedThreads(records);
		}
	    }
	}
    }


    Scheduler getScheduler(int lane) 
    {
	return schedulers[lane];
    }

    String getName() 
    {
	return name;
    }

    ThreadPool getPool(int lane) 
    {
	return pools[lane];
    }

}
