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
	    return (targetPercentage-totalTime/grandTotal)/targetPercentage;
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
	
	SubSlice(TimeSlice parent, long expiration) {
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
	    record = new UsageRecord();
	    record.child = child;
	    records.put(child, record);
	    record.targetPercentage = 
		PropertyParser.getDouble(properties, child.getName(), .01);
	}
	return record;
    }

    private TreeSet rankChildren() {
	TreeSet orderedChildren = new TreeSet(comparator);
	orderedChildren.addAll(treeNode.getChildren());
	return orderedChildren;
    }


    final private long DICE_SIZE = 250;

    public synchronized TimeSlice getSlice(TimeSliceConsumer consumer) 
    {
	
	TimeSlice slice = null;

	if (isRoot())
	    slice = getLocalSlice(this);
	else
	    slice = treeNode.getParentPolicy().getSlice(this);

	if (slice == null) return null;

	SubSlice piece = new SubSlice(slice, DICE_SIZE);
	piece.in_use = true;

	TreeSet kids = rankChildren();
	Iterator itr = kids.iterator();
	while (itr.hasNext()) {
	    TimeSliceConsumer kid = (TimeSliceConsumer) itr.next();
	    if (kid == consumer) return piece;
	    if (kid.offerSlice(piece)) return null;
	}

	// No one wants it
	throw new RuntimeException("Zinky says this is impossible.");
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
	UsageRecord record = getUsageRecord(consumer);
	record.totalTime += elapsed;
	++record.count;
	totalTime += elapsed;



	if (whole.isExpired()) {
	    if (isRoot())
		releaseLocalSlice(this, slice);
	    else
		treeNode.getParentPolicy().releaseSlice(this, whole);
	} else {
	    offerSlice(whole);
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
	    if (kid.offerSlice(piece)) return true;
	}
	return false;
    }

}
		
