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

package org.cougaar.core.qos.metrics;

import java.util.LinkedList;

public abstract class DecayingHistory
{
    
    public abstract void newAddition(String period, 
				     SnapShot addition, 
				     SnapShot last);


    public static class SnapShot {
	public long timestamp;

	public SnapShot() {
	    timestamp = System.currentTimeMillis();
	}
    }
	    


    private class DecayingHistoryList extends LinkedList {
	private int column;
	private String period;
	private int length;
	private int index;
	private boolean full;

	DecayingHistoryList(int column, int length) {
	    this.column = column;
	    this.length = length;
	    period = Math.round(Math.pow(length, column+1)) + "SecAvg";
	    index = 0;
	    full = false;
	}


	public boolean add(SnapShot x, SnapShot new_elt) {
	    // Do 1 second average
	    if (column == 0 && !isEmpty()) {
		SnapShot first = (SnapShot) getFirst();
		newAddition("1SecAvg", new_elt, first);
	    }

	    SnapShot last = null;
	    if (full) last = (SnapShot) removeLast();	
	    addFirst(x);

	    // If the list isn't full yet, 'last' hasn't been set.
	    // Set it now,
	    if (last == null) last = (SnapShot) getLast();

	    // Notify the listener
	    // But listener column tags are incremented by one
	    newAddition(period, new_elt, last);

	    boolean shift = ++index == length;
	    if (shift) {
		DecayingHistoryList next = getList(column+1);
		if (next != null) next.add(last, new_elt);
		full = true;
		index = 0;
	    } 
	    return true;
	}
    }


 
    private DecayingHistoryList[] data;
    private int total_count = 0;

    public DecayingHistory(int rows, int columns)
    {
	data = new DecayingHistoryList[columns];
	for (int i=0; i<columns; i++) {
	    data[i] = new DecayingHistoryList(i, rows);
	}
    }

    private DecayingHistoryList getList(int index) {
	if (index < data.length)
	    return data[index];
	else
	    return null;
    }

    public synchronized void add(SnapShot x) {
	data[0].add(x, x);
	++total_count;
    }

    


}
