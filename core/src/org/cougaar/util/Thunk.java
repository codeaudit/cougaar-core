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

package org.cougaar.util;

import java.util.*;

/** 
 * A Thunk is small piece of code to be executed repeatedly, often
 * gathering state for later perusal.
 */

public interface Thunk 
{
  /** Called to "run" the thunk on an object **/
  void apply(Object o);

  /** A counter thunk which counts the number of times it is called.
   * May be reused via reset() method, but no attempt is made to
   * make instances thread-safe.
   **/
  class Counter implements Thunk {
    private int counter = 0;
    public Counter() {}
    public void apply(Object o) {
      counter++;
    }
    public int getCount() { return counter; }
    public void reset() { counter=0;}
  }

  /** a Thunk which collects all the arguments into a Collection **/
  class Collector implements Thunk {
    private final Collection c;
    public Collector() { c = new ArrayList(); }
    public Collector(Collection c) { this.c=c; }
    public void apply(Object o) { c.add(o); }
    public Collection getCollection() { return c; }
  }

}

