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

import org.cougaar.util.PropertyParser;

import java.io.FileInputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeSet;

public class PercentageLatencyPolicy
    extends DefaultTimeSlicePolicy
{
    private static class UsageRecord {
	TimeSliceConsumer child;
	long totalTime;
	double targetPercentage;
	int count;

	double distance(double grandTotal) {
	    double d =
		(targetPercentage-totalTime/grandTotal)/targetPercentage;
// 	    System.out.println(child+ 
// 			       " distance=" +d+
// 			       " count=" +count+
// 			       " target=" +targetPercentage+
// 			       " time=" +totalTime);
	    return d;
	}

    }

    private class PercentageComparator implements Comparator {
	public boolean equals(Object x) {
	    return x == this;
	}

	// Use this when there's no other way to compare two Consumers
	private int hashCompare(Object o1, Object o2) {
	    if (o1.hashCode() < o2.hashCode())
		return -1;
	    else
		return 1;
	}

	public int compare(Object o1, Object o2) {
	    if (o1 == o2) return 0;

	    TimeSliceConsumer x = (TimeSliceConsumer) o1;
	    TimeSliceConsumer y = (TimeSliceConsumer) o2;
	    UsageRecord x_usage = getUsageRecord(x);
	    UsageRecord y_usage = getUsageRecord(y);

	    if (x_usage == null && y_usage == null) {
		return hashCompare(o1, o2);
	    } else if (x_usage == null) {
		return -1;
	    } else if (y_usage == null) {
		return 1;
	    }

	    double x_distance = x_usage.distance(totalTime);
	    double y_distance = y_usage.distance(totalTime);
			

	    // Smaller distances are less preferable
	    if (x_distance < y_distance)
		return 1;
	    else if (x_distance > y_distance) 
		return -1;
	    else 
		return hashCompare(o1, o2);
	}

    }

    private Properties properties;
    private Comparator comparator;
    private HashMap records;
    private long totalTime;

    public PercentageLatencyPolicy(String propertiesFilename) {
	super();
	
	properties = new Properties();
	records = new HashMap();
	try {
	    FileInputStream fos = new FileInputStream(propertiesFilename);
	    properties.load(fos);
	    fos.close();
	} catch (java.io.IOException ex) {
	    ex.printStackTrace();
	}

	comparator = new PercentageComparator();
	
    }

    String getPolicyID () {
	return "PercentageLatencyPolicy";
    }

    private TimeSlice makeSubSlice(TimeSlice parent, long expiration) {
	TimeSlice slice = new TimeSlice(this, parent);
	slice.start = System.currentTimeMillis();
	slice.end = Math.min(parent.end, slice.start + expiration);
	return slice;
    }


    private synchronized UsageRecord getUsageRecord(TimeSliceConsumer child) {
	UsageRecord record = (UsageRecord) records.get(child);
	if (record == null) {
	    if (CougaarThread.Debug)
		System.out.println("Creating new UsageRecord for " +child);
	    record = new UsageRecord();
	    records.put(child, record);
	    record.child = child;
	    record.targetPercentage = 
		PropertyParser.getDouble(properties, child.getName(), .01);
	}
	return record;
    }


    // Generate a new TreeSet everytime.  Ridiculously inefficient but
    // easy to write...
    private TreeSet rankChildren() {
	TreeSet orderedChildren = new TreeSet(comparator);
	Iterator itr = treeNode().getChildren().iterator();
	TreeNode childTreeNode = null;
	while (itr.hasNext()) {
	    childTreeNode = (TreeNode) itr.next();
	    orderedChildren.add(childTreeNode.getPolicy());
	}
	orderedChildren.add(treeNode().getScheduler());
	return orderedChildren;
    }



    private void noteRelease(TimeSliceConsumer consumer,
			     TimeSlice slice)
    {
	long elapsed = System.currentTimeMillis()-slice.run_start;
	UsageRecord record = getUsageRecord(consumer);

	record.totalTime += elapsed;
	++record.count;
	totalTime += elapsed;

	// 	System.out.println(consumer+ 
	// 			   " usage=" +record.totalTime+
	// 			   " count=" +record.count+
	// 			   " total=" +totalTime);

    }

    public void noteChangeOfOwnership(TimeSliceConsumer consumer,
				      TimeSlice slice)
    {
	noteRelease(consumer, slice);
	TimeSlicePolicy parent = treeNode().getParentPolicy();
	if (parent != null) {
	    parent.noteChangeOfOwnership(this, slice.parent);
	}

    }
    
    private final long DICE_SIZE = 250;

    public synchronized TimeSlice getSlice(TimeSliceConsumer consumer) 
    {
	
	TimeSlice slice = null;

	if (CougaarThread.Debug)
	    System.out.println(this+ " getting slice for " +consumer);

	if (isRoot())
	    slice = getLocalSlice(this);
	else
	    slice = treeNode().getParentPolicy().getSlice(this);

	if (slice == null) {
	    if(CougaarThread.Debug)
		System.out.println("No slice available");
	    return null;
	}

	TimeSlice piece = makeSubSlice(slice, DICE_SIZE);
	piece.in_use = true;

	TreeSet kids = rankChildren();
	Iterator itr = kids.iterator();
	while (itr.hasNext()) {
	    TimeSliceConsumer kid = (TimeSliceConsumer) itr.next();
	    if (kid == consumer) {
		if (CougaarThread.Debug)
		    System.out.println("Giving slice to preferred consumer " +
				       kid);
		return piece;
	    } else if (kid.offerSlice(piece)) {
		if (CougaarThread.Debug)
		    System.out.println("Giving slice to " + kid);
		return null;
	    } else {
		if (CougaarThread.Debug)
		    System.out.println("Not giving slice to " + kid);
	    }
	}

	System.err.println("getSlice() found no takers");
	return null;
    }


    public synchronized void releaseSlice(TimeSliceConsumer consumer, 
					  TimeSlice slice) 
    {
	if (slice.owner != this) {
	    System.err.println(this+ " asked to release a slice owned by " 
				+slice.owner);
	    Thread.dumpStack();
	    return;
	}

	if (CougaarThread.Debug)
	    System.out.println("Releasing slice from " +consumer);


	noteRelease(consumer, slice);

	TimeSlice whole = slice.parent;

	// If the slice is now expired, or if no one wants it right
	// now, give it back.  This must always operate on the full
	// slice, as given by our parent, not on the locally created
	// subslice.
	if (whole.isExpired() || !offerSlice(whole)) {
	    if (isRoot())
		releaseLocalSlice(this, whole);
	    else
		treeNode().getParentPolicy().releaseSlice(this, whole);
	}
    }

    public synchronized boolean offerSlice(TimeSlice slice) 
    {
	TimeSlice piece = makeSubSlice(slice, DICE_SIZE);
	piece.in_use = true;

	TreeSet kids = rankChildren();
	Iterator itr = kids.iterator();
	while (itr.hasNext()) {	
	    TimeSliceConsumer kid = (TimeSliceConsumer) itr.next();
	    if (kid.offerSlice(piece)) {
		if (CougaarThread.Debug)
		    System.out.println("Slice accepted by " + kid);
		return true;
	    } else {
		if (CougaarThread.Debug)
		    System.out.println("Slice refused by " + kid);
	    }
	}
	return false;
    }

}
		
