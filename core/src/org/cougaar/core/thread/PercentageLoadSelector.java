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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimerTask;
import java.util.TreeSet;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.PropertyParser;

/**
 * A sample RightsSelector that attempts to select among the children
 * in such a way as to match a set of target percentages.
 * 
 * @property org.cougaar.thread.targets Specifies a file which
 * contains percentage targets for the children.
 */
public class PercentageLoadSelector
    extends RoundRobinSelector
    implements ThreadListener
{
    private class ConsumerRecord {
	String name;
	int outstanding;
	long timestamp;
	double accumulator;
	long snapshot_timestamp;
	double snapshot_accumulator;
	double rate;
	double targetPercentage;

	ConsumerRecord(String name) {
	    this.name = name;
	    targetPercentage = 	
		PropertyParser.getDouble(properties, name, .05);
	    System.err.println(name+ " target=" +targetPercentage);
	}


	synchronized void snapshot() {
	    long now = System.currentTimeMillis();
	    rate = (accumulator-snapshot_accumulator)/
		(now-snapshot_timestamp);
	    snapshot_timestamp = now;
	    snapshot_accumulator = accumulator;
	    System.out.println(name+ " rate=" +rate);
	}


	double distance() {
	    return (targetPercentage-rate)/targetPercentage;
	}

	synchronized void accumulate() {
	    long now = System.currentTimeMillis();
	    if (timestamp > 0) {
		double deltaT = now - timestamp;
		accumulator += deltaT * outstanding;
	    } 
	    timestamp = now;
	}
    }

    private class SnapShotter extends TimerTask {
	public void run() {
	    synchronized (records) {
		Iterator itr = records.values().iterator();
		while (itr.hasNext()) {
		    ConsumerRecord rec = (ConsumerRecord) itr.next();
		    rec.snapshot();
		}
	    }
	    rankChildren();
	}
    }


    private class DistanceComparator implements Comparator {
	private int hashCompare(Object o1, Object o2) {
	    if (o1.hashCode() < o2.hashCode())
		return -1;
	    else
		return 1;
	}

	public int compare(Object o1, Object o2) {
	    if (o1 == o2) return 0;

	    Scheduler x = (Scheduler) o1;
	    Scheduler y = (Scheduler) o2;
	    double x_distance = getSchedulerDistance(x);
	    double y_distance = getSchedulerDistance(y);
			

	    // Smaller distances are less preferable
	    if (x_distance < y_distance)
		return 1;
	    else if (x_distance > y_distance) 
		return -1;
	    else 
		return hashCompare(o1, o2);
	}

    }

    private static final String TARGETS_PROP =
	"org.cougaar.thread.targets";
    private HashMap records = new HashMap();
    private Properties properties = new Properties();
    private int total = 0;
    private Comparator comparator;
    private TreeSet orderedChildren;


    public PercentageLoadSelector(ServiceBroker sb) {
	String propertiesFilename = System.getProperty(TARGETS_PROP);
	if (propertiesFilename != null) {
	    try {
		FileInputStream fos = new FileInputStream(propertiesFilename);
		properties.load(fos);
		fos.close();
	    } catch (java.io.IOException ex) {
		ex.printStackTrace();
	    }
	}

	comparator = new DistanceComparator();


	ThreadListenerService tls = (ThreadListenerService)
	    sb.getService(this, ThreadListenerService.class, null);
	tls.addListener(this);
	ThreadService ts = (ThreadService)
	    sb.getService(this, ThreadService.class, null);
	ts.schedule(new SnapShotter(), 5000, 1000);
    }

    ConsumerRecord findRecord(String name) {
	ConsumerRecord rec = null;
	synchronized (records) {
	    rec = (ConsumerRecord) records.get(name);
	    if (rec == null) {
		rec = new ConsumerRecord(name);
		records.put(name, rec);
	    }
	}
	return rec;
    }


    public void threadQueued(Schedulable schedulable, 
			     Object consumer) 
    {
    }
    public void threadDequeued(Schedulable schedulable, 
			       Object consumer)
    {
    }
    public void threadStarted(Schedulable schedulable, 
			      Object consumer)
    {
    }
    public void threadStopped(Schedulable schedulable, 
			      Object consumer)
    {
    }

    public void rightGiven(String consumer) {
	++total;
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	++rec.outstanding;
    }
		
    public void rightReturned(String consumer) {
	--total; 
	ConsumerRecord rec = findRecord(consumer);
	rec.accumulate();
	--rec.outstanding;
   }



    private double getSchedulerDistance(Scheduler scheduler) {
	ConsumerRecord rec = findRecord(scheduler.getName());
	if (rec != null)
	    return rec.distance();
	else
	    return 1;
    }

    // RightsSelector

    // Too inefficient to use but simple to write...
    private void rankChildren() {
	TreeSet children = new TreeSet(comparator);
	Iterator itr = scheduler.getTreeNode().getChildren().iterator();
	TreeNode child = null;
	while (itr.hasNext()) {
	    child = (TreeNode) itr.next();
	    children.add(child.getScheduler());
	}
	children.add(scheduler);
	orderedChildren = children;
    }

    public SchedulableObject getNextPending() {
	if (orderedChildren == null) {
	    // Snapshotter hasn't run yet.  Round-robin instead.
	    return super.getNextPending();
	}
	// Choose the one with the largest distance()
	Iterator itr = orderedChildren.iterator();
	Scheduler next = null;
	SchedulableObject handoff = null;
	while(itr.hasNext()) {
	    next = (Scheduler) itr.next();
	    // The list contains the scheduler itself as well as its
	    // children, In the former case we can't call
	    // getNextPending since that will recurse forever.  We
	    // need the super method, conveniently available as
	    // getNextPendingSuper.
	    if (next == scheduler)
		handoff = scheduler.popQueue();
	    else
		handoff = next.getNextPending();
	    if (handoff != null) {
		// If we're the parent of the Scheduler to which the
		// handoff is given, increase the local count.
		if (next != scheduler) scheduler.incrementRunCount(next);
		return handoff;
	    }
	}
	return null;
    }

}
