/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.util.*;
import java.io.Serializable;

/**
 * A Collection which implements a set of TimeSpan elements
 * which are maintained sorted first by start time and then by
 * end time.  The order of temporally-equivalent but non-equal
 * objects is undefined but stable.
 **/
public class TimeSpanSet 
  extends ArrayListFoundation
  implements SortedSet, Serializable
{
  // constructors
  public TimeSpanSet() {
    super();
  }

  public TimeSpanSet(int i) {
    super(i);
  }

  public TimeSpanSet(Collection c) {
    super(c.size());
    
    addAll(c);
  }

  public TimeSpanSet(TimeSpanSet t) {
    super(t.size());

    unsafeUpdate(t);
  }


  public boolean add(Object o) {
    if (! (o instanceof TimeSpan)) 
      throw new IllegalArgumentException();
    TimeSpan timeSpan = (TimeSpan)o;

    int i = search(timeSpan);
    long startTime = timeSpan.getStartTime();
    long endTime = timeSpan.getEndTime();

    // Make sure that it's not really equal to any existing members of the set
    while ((i<size) &&
           (compare(startTime, endTime, (TimeSpan)elementData[i])== 0)){
      if (timeSpan.equals(elementData[i++])) return false;
    }

    super.add(i, timeSpan);
    return true;
  }

  public void add(int i, Object o) {
    throw new UnsupportedOperationException("TimeSpanSet.add(int index, Object o) is not supported."); 
  }

  public boolean addAll(Collection c) {
    boolean hasChanged = false;

    for (Iterator i = c.iterator(); i.hasNext(); ) {
      if (add(i.next()))
        hasChanged = true;
    }
    return hasChanged;
  }

  public boolean addAll(int index, Collection c) {
    throw new UnsupportedOperationException("TimeSpanSet.addAll(int index, Collection c) is not supported."); 
  }

  public boolean contains(Object o) {
    if (o instanceof TimeSpan) {
      return find((TimeSpan)o) != -1;
    }
    return false;
  }

  public boolean remove(Object o) {
    // find it faster
    if (o instanceof TimeSpan) {
      int i = find((TimeSpan)o);
      if (i == -1) return false;
      super.remove(i);
      return true;
    } else {
      return false;
    }
  }

  public Object set(int index, Object element) {
    throw new UnsupportedOperationException("TimeSpanSet.set(int index, Object element) is not supported."); 
  }

  public String toString() {
    // do we want to change the implementation here?
    return super.toString();
  }

  // SortedSet implementation
  public Comparator comparator() {
    return myComparator;
  }
  public final Object first() {
    return (size>0)?(elementData[0]):null;
  }
  public final SortedSet headSet(Object toElement){
    throw new UnsupportedOperationException();
  }
  public Object last() {
    int l = size;
    return (l > 0)?elementData[l-1]:null;
  }
  public final SortedSet subSet(Object fromElement, Object toElement) {
    throw new UnsupportedOperationException();
  }
  public final SortedSet tailSet(Object fromElement) {
    throw new UnsupportedOperationException();
  }

  /** generic timespan comparison **/
  public static final int compare(TimeSpan p1, TimeSpan p2) {
    int compare;

    if (p2.getStartTime() != p1.getStartTime()) {
      compare = (p2.getStartTime() > p1.getStartTime()) ? 1 : -1;
    } else if (p2.getEndTime() != p1.getEndTime()) {
      compare = (p2.getEndTime() > p1.getEndTime()) ? 1 : -1;
    } else if (p2.hashCode() != p2.hashCode()) {
      compare = (p2.hashCode() > p1.hashCode()) ? 1 : -1 ;
    } else {
      compare = 0;
    }

    return compare;
  }

  /** optimization for non-comparator use **/
  public static final int compare(long p1s, long p1e, TimeSpan p2) {
    int compare;

    if (p2.getStartTime() != p1s) {
      compare = (p2.getStartTime() > p1s) ? 1 : -1;
    } else if (p2.getEndTime() != p1e) {
      compare = (p2.getEndTime() > p1e) ? 1 : -1;
    } else {
      compare = 0;
    }
    
    return compare;
  }

  /** comparator for Collection use **/
  private static final Comparator myComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
        if (o1 instanceof TimeSpan &&
            o2 instanceof TimeSpan) {
          return TimeSpanSet.compare((TimeSpan)o1,(TimeSpan)o2);
        } else {
          return 0;
        }
      }
    };


  /** @return the intersecting Element with the smallest timespan.
   * The result is undefined if there is a tie for smallest and null 
   * if there are no elements.
   **/
  public Object getMinimalIntersectingElement(final long time) {
    int l = size;
    if (l==0) return null;

    TimeSpan best = null;
    long bestd = TimeSpan.MAX_VALUE-TimeSpan.MIN_VALUE;

    for (int i = 0; i<l; i++) {
      TimeSpan ts = (TimeSpan) elementData[i];
      long t0 = ts.getStartTime();
      if (time<t0) break; // if the element is after our point, we're done

      long t1 = ts.getEndTime();
      if (t1<time) continue; // if the point is after the endpoint, we continue

      long d=t1-t0;
      if (best ==null ||
          (d < bestd) ) {
        best = ts;
        bestd = d;
      }
    }
    return best;
  }

  /** @return the subset of elements which meet the specified predicate.
   **/
  public Collection filter(UnaryPredicate predicate) {
    return Filters.filter(this, predicate);
  }

  /** @return the subset of elements which intersect with 
   * the specified time.
   **/
  public final Collection intersectingSet(final long time) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          TimeSpan ts = (TimeSpan) o;
          return (time >= ts.getStartTime() &&
                  time < ts.getEndTime());
        }
      });
  }

  /** @return the subset of elements which intersect with the
   * specified time span.
   **/
  public final Collection intersectingSet(final long startTime, 
                                          final long endTime) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          TimeSpan ts = (TimeSpan) o;
          return (ts.getStartTime()<endTime &&
                  ts.getEndTime()>startTime);
        }
      });
  }
  
  /** @return the subset of elements which intersect with the
   * specified time span.
   **/
  public final Collection intersectingSet(TimeSpan span) {
    return intersectingSet(span.getStartTime(), span.getEndTime());
  }

  /** @return the subset of elements which are completely enclosed
   * by the specified time span.
   **/
  public final Collection encapsulatedSet(final long startTime, 
                                          final long endTime) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          TimeSpan ts = (TimeSpan) o;
          return (ts.getStartTime()>=startTime &&
                  ts.getEndTime()<=endTime);
        }
      });
  }

  /** @return the subset of elements which are completely enclosed
   * by the specified time span.
   **/
  public final Collection encapsulatedSet(TimeSpan span) {
    return encapsulatedSet(span.getStartTime(), span.getEndTime());
  }

  /** @return the subset of elements which completely enclose
   * the specified time span.
   **/
  public final Collection encapsulatingSet(final long startTime, 
                                           final long endTime) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object o) {
          TimeSpan ts = (TimeSpan) o;
          return (startTime <= ts.getStartTime() &&
                  endTime >= ts.getEndTime());
        }
      });
  }
    
  /** @return the subset of elements which completely enclose
   * the specified time span.
   **/
  public final Collection encapsulatingSet(TimeSpan span) {
    return encapsulatingSet(span.getStartTime(), span.getEndTime());
  }

  // private support

  /** 
   * unsafeUpdate - replaces all elements with specified Collection
   * Should only be used if c has already been validated.
   * @returns boolean - true if any elements added else false.
   */
  protected boolean unsafeUpdate(Collection c) {
    clear();
    return super.addAll(c);
  }

  /** @return the index of the object in the list or -1 **/
  protected final int find(TimeSpan o) {
    // we should really use a boolean search rather
    // than iterating through
    int l = size;
    for (int i = 0; i<l; i++) {
      if (o.equals(elementData[i])) return i;
    }
    return -1;
  }

  /** @return the index of the first object in the list which is not
   * less than the specified object. If there are no elements or
   * all elements are less than the specified timespan, will
   * return the length of the list.
   **/
  protected final int search(TimeSpan o) {
    return search(o.getStartTime(),o.getEndTime());
  }

  /** @return the index of the first object in the list which is not
   * less than the specified span. If there are no elements or
   * all elements are less than the specified timespan, will
   * return the length of the list.
   **/
  protected final int search(long t0, long t1) {
    int l = size;
    if (l==0) return 0;         // bail if empty

    // this is a crock - we should do a binary search.
    for (int i = 0; i < l; i++) {
      if (compare(t0, t1, (TimeSpan) elementData[i]) >= 0)
        return i;
    }
    return l;
  }

  public static void main(String arg[]) {
    class X implements TimeSpan {
      long start;
      long end;
      String text;

      public X(long x, String t) { start= x; end = x + 1; text = t;} 
      public long getStartTime() { return start; }
      public long getEndTime() { return end;}
      public void setTimeSpan(long s, long e) {start = s; end = e;}
      public String getText() { return text;}
      public void setText(String t) { text = t; }
      public String toString() { return "{start = " + start + 
                                   " end = " + end + 
                                   " text = " + text + "}";}
    }
    
    TimeSpanSet tss = new TimeSpanSet();

    /*
    for (int i = 20; i >6; i-=2) {
      tss.add(new X(i));
    }
    */

    if (!tss.add(new X(TimeSpan.MIN_VALUE, "milk"))) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }
    if (!tss.add(new X(TimeSpan.MIN_VALUE, "dark"))) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }

    X forever = new X(TimeSpan.MIN_VALUE, "bittersweet");
    forever.setTimeSpan(forever.getStartTime(), TimeSpan.MAX_VALUE);
    if (!tss.add(forever)) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }

    X longTime = new X(TimeSpan.MIN_VALUE, "semisweet");
    longTime.setTimeSpan(forever.getStartTime() + TimeSpan.EPSILON,
                         TimeSpan.MAX_VALUE);
    if (!tss.add(longTime)) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }




    X midEpoch3 = new X(TimeSpan.MIN_VALUE, "hazelnut");
    midEpoch3.setTimeSpan(midEpoch3.getStartTime() + TimeSpan.EPSILON,
                         10);
    if (!tss.add(midEpoch3)) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }


    X midEpoch1 = new X(9, "truffle");
    midEpoch1.setTimeSpan(midEpoch1.getStartTime(), TimeSpan.MAX_VALUE);
    if (!tss.add(midEpoch1)) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }


    X midEpoch2 = new X(9, "caramel");
    midEpoch2.setTimeSpan(midEpoch2.getStartTime(), TimeSpan.MAX_VALUE);
    if (!tss.add(midEpoch2)) {
      System.out.println("Unable to add");
      IllegalArgumentException iae = 
        new IllegalArgumentException("Unable to add");
      iae.printStackTrace();
    }

    
    for (int i = 3; i < 16; i+=2) { 
      if (!tss.add(new X(i, Long.toString(i)))) {
        System.out.println("Unable to add");
        IllegalArgumentException iae = 
          new IllegalArgumentException("Unable to add");
        iae.printStackTrace();
      }
    }
    

    System.err.println(tss.toString());

    for (int i = 0; i<23; i++) {
      System.out.println("search("+i+") = "+tss.search(i,i+1));
      System.out.flush();
    }
  }

}
  
  
