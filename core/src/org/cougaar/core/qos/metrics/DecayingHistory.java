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

package org.cougaar.core.qos.metrics;

import java.util.LinkedList;

public class DecayingHistory
{
    
    public interface Callback {
	void newAdditionLast(Object addition, Object last);
	void newAdditionHistory(int column, Object addition, Object last);
    }



   private class DecayingHistoryList extends LinkedList {
	private int column;
	private int length;
	private int index;
	private boolean full;

	DecayingHistoryList(int column, int length) {
	    this.column = column;
	    this.length = length;
	    index = 0;
	    full = false;
	}


	public boolean add(Object x, Object new_elt) {
	    // Do 1 second average
	    if (column == 0 && !isEmpty()) {
		Object first = getFirst();
		callback.newAdditionLast(new_elt, first);
	    }

	    Object last = null;
	    if (full) last = removeLast();	
	    addFirst(x);

	    // If the list isn't full yet, 'last' hasn't been set.
	    // Set it now,
	    if (last == null) last = getLast();

	    // Notify the listener
	    // But listener column tags are incremented by one
	    callback.newAdditionHistory(column, new_elt, last);

	    boolean shift = ++index == length;
	    if (shift) {
		DecayingHistoryList next = getList(column+1);
		next.add(last, new_elt);
		full = true;
		index = 0;
	    } 
	    return true;
	}
    }


 
    private DecayingHistoryList[] data;
    private int total_count = 0;
    private Callback callback;

    public DecayingHistory(int rows, int columns, Callback callback)
    {
	data = new DecayingHistoryList[columns];
	for (int i=0; i<columns; i++) {
	    data[i] = new DecayingHistoryList(i, rows);
	}
	this.callback = callback;
    }

    private DecayingHistoryList getList(int index) {
	if (index < data.length)
	    return data[index];
	else
	    return null;
    }

    public synchronized void add(Object x) {
	data[0].add(x, x);
	++total_count;
    }



}
