/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
  public static class Counter implements Thunk {
    private int counter = 0;
    public Counter() {}
    public void apply(Object o) {
      counter++;
    }
    public int getCount() { return counter; }
    public void reset() { counter=0;}
  }

  /** a Thunk which collects all the arguments into a Collection **/
  public static class Collector implements Thunk {
    private final Collection c;
    public Collector() { c = new ArrayList(); }
    public Collector(Collection c) { this.c=c; }
    public void apply(Object o) { c.add(o); }
    public Collection getCollection() { return c; }
  }

}

