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
	    return
		(targetPercentage-totalTime/grandTotal)/targetPercentage;
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

    private static class SubSlice extends TimeSlice {
	TimeSlice parent;
	TimeSliceConsumer consumer;
	
	SubSlice(TimeSlice parent, 
		 long expiration) 
	{
	    start = System.currentTimeMillis();
	    end = Math.min(parent.end, start + expiration);
	    this.parent = parent;
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

    private synchronized UsageRecord getUsageRecord(TimeSliceConsumer child) {
	UsageRecord record = (UsageRecord) records.get(child);
	if (record == null) {
	    if (Scheduler.DebugThreads)
		System.out.println("Creating new UsageRecord for " +child);
	    record = new UsageRecord();
	    records.put(child, record);
	    record.child = child;
	    record.targetPercentage = 
		PropertyParser.getDouble(properties, child.getName(), .01);
	}
	return record;
    }

    private TreeSet rankChildren() {
	TreeSet orderedChildren = new TreeSet(comparator);
	Iterator itr = treeNode.getChildren().iterator();
	PolicyTreeNode childTreeNode = null;
	while (itr.hasNext()) {
	    childTreeNode = (PolicyTreeNode) itr.next();
	    orderedChildren.add(childTreeNode.getPolicy());
	}
	orderedChildren.add(treeNode.getScheduler());
	return orderedChildren;
    }


    final private long DICE_SIZE = 250;

    public synchronized TimeSlice getSlice(TimeSliceConsumer consumer) 
    {
	
	TimeSlice slice = null;

	if (Scheduler.DebugThreads)
	    System.out.println(this+ " getting slice for " +consumer);

	if (isRoot())
	    slice = getLocalSlice(this);
	else
	    slice = treeNode.getParentPolicy().getSlice(this);

	if (slice == null) {
	    if(Scheduler.DebugThreads)
		System.out.println("No slice available");
	    return null;
	}

	SubSlice piece = new SubSlice(slice, DICE_SIZE);
	piece.in_use = true;

	TreeSet kids = rankChildren();
	Iterator itr = kids.iterator();
	while (itr.hasNext()) {
	    TimeSliceConsumer kid = (TimeSliceConsumer) itr.next();
	    if (kid == consumer) {
		if (Scheduler.DebugThreads)
		    System.out.println("Giving slice to preferred consumer " +
				       kid);
		piece.consumer = consumer;
		return piece;
	    } else if (kid.offerSlice(piece)) {
		if (Scheduler.DebugThreads)
		    System.out.println("Giving slice to " + kid);
		piece.consumer = kid;
		return null;
	    } else {
		if (Scheduler.DebugThreads)
		    System.out.println("Not giving slice to " + kid);
	    }
	}

	System.err.println("getSlice() found no takers");
	return null;
    }


    public synchronized void releaseSlice(TimeSliceConsumer consumer, 
					  TimeSlice slice) 
    {
	if (!(slice instanceof SubSlice) ) {
	    System.err.println(slice + " is not a SubSlice");
	    return;
	}

	long elapsed = System.currentTimeMillis()-slice.start;
	SubSlice piece = (SubSlice) slice;
	TimeSlice whole = piece.parent;
	UsageRecord record = getUsageRecord(piece.consumer);


	if (Scheduler.DebugThreads)
	    System.out.println("Releasing slice from " +consumer+
			       " (" +piece.consumer+ ")");


	record.totalTime += elapsed;
	++record.count;
	totalTime += elapsed;

	// If the slice is now expired, or if no one wants it right
	// now, give it back.  This must always operate on the full
	// slice, as given by our parent, not on the locally created
	// SubSlice.
	if (whole.isExpired() || !offerSlice(whole)) {
	    if (isRoot())
		releaseLocalSlice(this, whole);
	    else
		treeNode.getParentPolicy().releaseSlice(this, whole);
	}
    }

    public boolean offerSlice(TimeSlice slice) 
    {
	SubSlice piece = new SubSlice(slice, DICE_SIZE);
	piece.in_use = true;

	TreeSet kids = rankChildren();
	Iterator itr = kids.iterator();
	while (itr.hasNext()) {	
	    TimeSliceConsumer kid = (TimeSliceConsumer) itr.next();
	    if (kid.offerSlice(piece)) {
		if (Scheduler.DebugThreads)
		    System.out.println("Slice accepted by " + kid);
		piece.consumer = kid;
		piece.start = System.currentTimeMillis();
		return true;
	    } else {
		if (Scheduler.DebugThreads)
		    System.out.println("Slice refused by " + kid);
	    }
	}
	return false;
    }

}
		
