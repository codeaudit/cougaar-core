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

import java.util.Comparator;

import org.cougaar.core.service.ThreadControlService;
import org.cougaar.util.UnaryPredicate;

class ThreadControlServiceProxy
    implements ThreadControlService
{
    private TreeNode node;

    ThreadControlServiceProxy(TreeNode node)
    {
	this.node = node;
    }


    public int getDefaultLane()
    {
	return node.getDefaultLane();
    }

    public void setDefaultLane(int lane)
    {
	node.setDefaultLane(lane);
    }

    private void validateLane(int lane)
    {
	if (lane < 0 || lane >= node.getLaneCount())
	    throw new RuntimeException("Lane is out of range: " +lane);
    }


    public void setMaxRunningThreadCount(int count, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setMaxRunningThreadCount(count);
    }

    public void setQueueComparator(Comparator comparator, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setQueueComparator(comparator);
    }

    public void setRightsSelector(RightsSelector selector, int lane)
    {
	validateLane(lane);
	node.getScheduler(lane).setRightsSelector(selector);
    }

    public boolean setQualifier(UnaryPredicate predicate, int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).setQualifier(predicate);
    }

    public boolean setChildQualifier(UnaryPredicate predicate, int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).setChildQualifier(predicate);
    }

    public int runningThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).runningThreadCount();
    }

    public int pendingThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).pendingThreadCount();
    }

    public int activeThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).activeThreadCount();
    }

    public int maxRunningThreadCount(int lane)
    {
	validateLane(lane);
	return node.getScheduler(lane).maxRunningThreadCount();
    }




    public void setMaxRunningThreadCount(int count)
    {
	setMaxRunningThreadCount(count, node.getDefaultLane());
    }

    public void setQueueComparator(Comparator comparator)
    {
	setQueueComparator(comparator, node.getDefaultLane());
    }

    public void setRightsSelector(RightsSelector selector)
    {
	setRightsSelector(selector, node.getDefaultLane());
    }

    public boolean setQualifier(UnaryPredicate predicate)
    {
	return setQualifier(predicate, node.getDefaultLane());
    }

    public boolean setChildQualifier(UnaryPredicate predicate)
    {
	return setChildQualifier(predicate, node.getDefaultLane());
    }


    public int runningThreadCount()
    {
	return runningThreadCount(node.getDefaultLane());
    }

    public int pendingThreadCount()
    {
	return pendingThreadCount(node.getDefaultLane());
    }

    public int activeThreadCount()
    {
	return activeThreadCount(node.getDefaultLane());
    }

    public int maxRunningThreadCount()
    {
	return maxRunningThreadCount(node.getDefaultLane());
    }



}
