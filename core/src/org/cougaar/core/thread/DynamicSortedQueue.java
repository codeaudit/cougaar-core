/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.thread;

import java.util.ArrayList;
import java.util.Comparator;

import org.cougaar.util.UnaryPredicate;

/**
 * A simple queue, built on array list, that uses a Comparator to
 * determine which elements is next (the smallest, according to the
 * Comparator).  Note that this is not a Collection.  Also note that
 * the methods are not synchronized.  It's the caller's reponsibility
 * to handle synchronization.
 */
public class DynamicSortedQueue
{
    private Comparator comparator;
    private ArrayList store;
    


    public DynamicSortedQueue(Comparator comparator) {
	store = new ArrayList();
	this.comparator = comparator;
    }

    interface Processor {
	void process(Object thing);
    }

    // Utter and total hack.
    void processEach(Processor processor) {
	for (int i = 0, n = store.size(); i < n; i++) {
	    Object thing = store.get(i);
	    processor.process(thing);
	}
    }

    public ArrayList filter(UnaryPredicate predicate) {
	ArrayList result = new ArrayList();
	for (int i = 0, n = store.size(); i < n; i++) {
	    Object candidate = store.get(i);
	    if (!predicate.execute(candidate)) {
		result.add(candidate);
		store.remove(i);
                i--;
                n--;
	    }
	}
	return result;
    }

    public String toString() {
	return "<DQ[" +store.size()+ "] " +store.toString()+ ">";
    }


    public boolean contains(Object x) {
	return store.contains(x);
    }

    public void setComparator(Comparator comparator) {
	this.comparator = comparator;
    }

    public int size() {
	return store.size();
    }

    public boolean add(Object x) {
	if (store.contains(x)) return false;
	store.add(x);
	return true;
    }


    public void remove(Object x) {
	store.remove(x);
    }
	    

    public boolean isEmpty() {
	return store.isEmpty();
    }


    public Object next() {
	Object min = null;
	for (int i = 0, n = store.size(); i < n; i++) {
	    Object candidate = store.get(i);
	    if (min == null) {
		min = candidate;
	    } else {
		int comp = comparator.compare(min, candidate);
		if (comp > 0) min = candidate;
	    }
	}
	if (min != null) store.remove(min);
	return min;
    }
	    
}

