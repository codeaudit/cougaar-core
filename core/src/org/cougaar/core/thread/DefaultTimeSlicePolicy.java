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

import org.cougaar.core.service.ThreadControlService;

import java.util.Iterator;


public class DefaultTimeSlicePolicy implements TimeSlicePolicy
{
    static final long DEFAULT_SLICE_DURATION = 1000;

    int outstandingChildSliceCount;
    TimeSlice[] timeSlices;
    PolicyTreeNode node;

    DefaultTimeSlicePolicy() {
	outstandingChildSliceCount = 0;
    }

    public void setNode(PolicyTreeNode node) {
	this.node = node;
	PolicyTreeNode parent = node.getParent();
	if (parent == null) {
	    // Root policy, make some shares
	    int maxRunningThreads = 
		node.getScheduler().maxRunningThreadCount();
	    timeSlices = new TimeSlice[maxRunningThreads];
	    for (int i=0; i<maxRunningThreads; i++) {
		timeSlices[i] = new TimeSlice();
	    }
	}
    }



    // Only the root does this
    TimeSlice findSlice() {
	TimeSlice slice = null;
	for (int i=0; i<timeSlices.length; i++) {
	    slice = timeSlices[i];
	    if (!slice.in_use) 	return slice;
	}
	return null;
    }


    public TimeSlice getSlice() {
	TimeSlicePolicy parent = node.getParentPolicy();
	if (parent != null) {
	    return parent.getSlice(this);
	} else {
	    System.err.println("Called getSlice() on the root policy!");
	    return null;
	}
    }

    public void releaseSlice(TimeSlice slice) {
	TimeSlicePolicy parent = node.getParentPolicy();
	if (parent != null) {
	    parent.releaseSlice(this, slice);
	} else {
	    System.err.println("Called releaseSlice() on the root policy!");
	}
    }


    public synchronized TimeSlice getSlice(TimeSlicePolicy child) {
	TimeSlice slice = null;
	TimeSlicePolicy parent = node.getParentPolicy();

	if (parent != null) {
	    slice =  parent.getSlice(this);
	} else {
	    // The root.
	    int use_count = node.getScheduler().runningThreadCount() +
		outstandingChildSliceCount;
	    if (use_count >= node.getScheduler().maxRunningThreadCount()) {
		if (Scheduler.DebugThreads)
		    System.out.println("No slices available from " 
				       +node.getName()+
				       " for " +child+
				       "; " +outstandingChildSliceCount+
				       " outstanding");
		return null;
	    }

	    slice = findSlice();
	    
	    if (slice != null) {
		++outstandingChildSliceCount;

		long start = System.currentTimeMillis();
		long end = start + DEFAULT_SLICE_DURATION;
		slice.start = start;
		slice.end = end;
		slice.in_use = true;
		
		if (Scheduler.DebugThreads)
		    System.out.println(node.getName() +
				       " made a slice for " +child+
				       "; " +outstandingChildSliceCount+
				       " now outstanding");
	    }
	}
	
	return slice;
    }

    public void releaseSlice(TimeSlicePolicy child, TimeSlice slice) {
	TimeSlicePolicy parent = node.getParentPolicy();
	slice.in_use = false;
	if (parent != null) {
	    parent.releaseSlice(this, slice);
	} else {
	    // The root of the tree: notify everyone that slices may be
	    // available.
	    --outstandingChildSliceCount;
	    if (Scheduler.DebugThreads)
		System.out.println(node.getName() +
				   " released a slice from " +child+
				   "; " +outstandingChildSliceCount+
				   " now outstanding");
	    if (!slice.isExpired()) offerSlice(slice);
	}
    }


    // Parent wants to give us a slice
    public boolean offerSlice(TimeSlice slice) {
	if (node.getScheduler().offerSlice(slice)) return true;
	// loop through children until one of them accepts
	Iterator itr = node.getChildren();
	while (itr.hasNext()) {
	    TimeSlicePolicy child = (TimeSlicePolicy) itr.next();
	    if (child.offerSlice(slice)) return true;
	}
	return false;

    }

}
