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

import java.util.ArrayList;
import java.util.Iterator;


public class DefaultTimeSlicePolicy implements TimeSlicePolicy
{
    static final long DEFAULT_SLICE_DURATION = 10000; // better

    private int outstandingChildSliceCount;
    private ArrayList timeSlices;
    private TreeNode treeNode;
    private String printString;

    DefaultTimeSlicePolicy() {
	outstandingChildSliceCount = 0;
    }


    TreeNode treeNode() {
	return treeNode;
    }
    

    public String toString() {
	return printString;
    }

    public String getName() {
	return treeNode.getName();
    }

    String getPolicyID () {
	return "DefaultTimeSlicePolicy";
    }

    public void  setMaxRunningThreadCount(int count) {
	makeSlices(count);
    }

    public void setTreeNode(TreeNode treeNode) {
	this.treeNode = treeNode;
	printString = "<" + getPolicyID() + " " + getName() + ">";
	TreeNode parent = treeNode.getParent();

	if (parent == null) {
	    // Root policy, make some shares
	    makeSlices(treeNode.getScheduler().maxRunningThreadCount());
	}
    }


    boolean isRoot() {
	return treeNode.getParent() == null;
    }


    synchronized void makeSlices(int count) {
	// count <= 0 means unlimited
	int size = count <= 0 ? 20 : count;
	timeSlices = new ArrayList(size);
	for (int i=0; i<size; i++) {
	    timeSlices.add(new TimeSlice(this));
	}
    }

    // Find a slice that's not in use
    TimeSlice findSlice() {
	TimeSlice slice = null;
	Iterator itr = timeSlices.iterator();
	while (itr.hasNext()) {
	    slice = (TimeSlice) itr.next();
	    if (!slice.in_use) return slice;
	}

	int max = treeNode.getScheduler().maxRunningThreadCount();
	if (max <= 0) {
	    // max <= 0 means unlimited
	    slice = new TimeSlice(this);
	    timeSlices.add(slice);
	    return slice;
	} else {
	    return null;
	}
    }


    public void noteChangeOfOwnership(TimeSliceConsumer consumer,
				      TimeSlice slice)
    {
	TimeSlicePolicy parent = treeNode.getParentPolicy();
	if (parent != null) parent.noteChangeOfOwnership(this, slice);
    }

    // Get a slice for a child.  This version passes the request all
    // the way to the root, which is the only owner of slices.
    public TimeSlice getSlice(TimeSliceConsumer consumer) {
	TimeSlicePolicy parent = treeNode.getParentPolicy();


	if (parent != null) {
	    TimeSlice result =  parent.getSlice(this);
	    return result;
	} else {
	    return getLocalSlice(consumer);
	}
    }


    void reinitializeSlice(TimeSlice slice) {
	slice.start = System.currentTimeMillis();
	slice.end = slice.start + DEFAULT_SLICE_DURATION;
    }

    private synchronized TimeSlice getLocalSlice(TimeSliceConsumer consumer) {
	int use_count = treeNode.getScheduler().runningThreadCount() +
	    outstandingChildSliceCount;
	int max = treeNode.getScheduler().maxRunningThreadCount();
	if (max > 0 && use_count >= max) {
	    if (CougaarThread.Debug)
		System.out.println("No slices available from " 
				   +this+
				   " for " +consumer+
				   "; " +outstandingChildSliceCount+
				   " outstanding");
	    return null;
	}

	TimeSlice slice = findSlice();
	    
	if (slice != null) {
	    ++outstandingChildSliceCount;

	    reinitializeSlice(slice);
	    slice.in_use = true;
		
	    if (CougaarThread.Debug)
		System.out.println(this +
				   " made a slice for " +consumer+
				   "; " +outstandingChildSliceCount+
				   " now outstanding");
	} else {
	    System.err.println("use_count < max running count but no slice available!");
	}
	
	return slice;
    }



    // Release a slice from a child.  This version passes it all the
    // way to the root of the tree, which is the only owner of slices.
    public void releaseSlice(TimeSliceConsumer consumer, 
					  TimeSlice slice) 
    {
	// If this is our slice and it expired, reset the start and
	// end times.  If the slice belongs to a parent, we don't mess
	// with it; in that case it could still be expired after this
	// statement.  It wonb't be offered to our children in that case.
	if (slice.owner == this && slice.isExpired()) reinitializeSlice(slice);

	// Re-offer the slice, unless it belongs to a parent and has
	// expired.
	boolean accepted = !slice.isExpired() && offerSlice(slice);

	if (accepted) {
	    // The slice was handed off to a child.  Nothing further to do,
	} else {
	    // No one wanted it (or we couldn't offer it because it
	    // was expired).  Give it back.
	    if (isRoot()) 
		releaseLocalSlice(this, slice);
	    else
		treeNode().getParentPolicy().releaseSlice(this, slice);
	}



    }



    private void releaseLocalSlice (TimeSliceConsumer consumer,
				    TimeSlice slice) 
    {
	// The root of the tree: notify everyone that slices may be
	// available.
	--outstandingChildSliceCount;
	if (CougaarThread.Debug)
	    System.out.println(this +
			       " released a slice from " +consumer+
			       "; " +outstandingChildSliceCount+
			       " now outstanding");


	if (CougaarThread.Debug)
	    System.out.println("Marking " +slice+ " as available");
	slice.in_use = false;
    }




    // Round-robin scheduling
    private int currentIndex = -1;
    private synchronized TimeSliceConsumer getNextConsumer() {
	TimeSliceConsumer result = null;
	ArrayList children = treeNode.getChildren();
	if (currentIndex== -1) {
	    result = treeNode.getScheduler();	
	    currentIndex = children.size() == 0 ? -1 : 0;
	} else {
	   TreeNode child =(TreeNode) children.get(currentIndex++);
	   result = child.getPolicy();
	   if (currentIndex == children.size()) currentIndex = -1;
	}

	return result;
    }



    // Parent wants to give us a slice. Offer it to consumers in
    // round-robin style.
    public synchronized boolean offerSlice(TimeSlice slice) {
	int lastIndex = currentIndex;
	while (true) {
	    TimeSliceConsumer consumer = getNextConsumer();
	    if (consumer.offerSlice(slice)) {
		if (CougaarThread.Debug)
		    System.out.println(consumer + " accepted offer");
		return true;
	    } else {
		if (CougaarThread.Debug)
		    System.out.println(consumer + " refused offer");
	    }
	    if (lastIndex == currentIndex) break;
	}

	return false;

    }

}
