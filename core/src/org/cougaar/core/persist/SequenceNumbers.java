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
package org.cougaar.core.persist;

class SequenceNumbers implements Comparable {
    int first = 0;
    int current = 0;
    long timestamp;             // The time the highest delta in this set was written.
    public SequenceNumbers() {
        timestamp = System.currentTimeMillis();
    }
    public SequenceNumbers(int first, int current, long timestamp) {
        this.first = first;
        this.current = current;
        this.timestamp = timestamp;
    }
    public SequenceNumbers(SequenceNumbers numbers) {
        this(numbers.first, numbers.current, numbers.timestamp);
    }
    public int compareTo(Object o) {
        SequenceNumbers that = (SequenceNumbers) o;
        if (this.timestamp < that.timestamp) return -1;
        if (this.timestamp > that.timestamp) return  1;
        return this.current = that.current;
    }
    public String toString() {
        return first + ".." + current;
    }
}
