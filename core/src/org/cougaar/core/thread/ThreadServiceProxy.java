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

import org.cougaar.core.service.ThreadService;

/**
 * The proxy implementation of Thread Service.
 */
final class ThreadServiceProxy 	implements ThreadService
{
    private TreeNode treeNode;

    ThreadServiceProxy(TreeNode treeNode) 
    {
	this.treeNode = treeNode;
    }

    TreeNode getTreeNode() {
	return treeNode;
    }




    public Schedulable getThread(Object consumer, Runnable runnable) {
	return new SchedulableObject(treeNode, runnable, null, consumer,
				     treeNode.getDefaultLane());
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name) 
    {
	return new SchedulableObject(treeNode, runnable, name, consumer,
				     treeNode.getDefaultLane());
    }

    public Schedulable getThread(Object consumer, 
				 Runnable runnable, 
				 String name,
				 int lane) 
    {
	if (lane < 0 || lane >= treeNode.getLaneCount())
	    throw new RuntimeException("Lane is out of range: " +lane);
	return new SchedulableObject(treeNode, runnable, name, consumer,
				     lane);
    }


    public void schedule(java.util.TimerTask task, long delay) {
	treeNode.timer().schedule(task, delay);
    }


    public void schedule(java.util.TimerTask task, long delay, long interval) {
	treeNode.timer().schedule(task, delay, interval);
    }

    public void scheduleAtFixedRate(java.util.TimerTask task, 
				    long delay, 
				    long interval)
    {
	treeNode.timer().scheduleAtFixedRate(task, delay, interval);
    }


}
