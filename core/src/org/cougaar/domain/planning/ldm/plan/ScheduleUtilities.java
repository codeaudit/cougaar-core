/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.ScheduleImpl;
import java.util.*;
import org.cougaar.util.*;

public final class ScheduleUtilities {
  public static final long millisperday = (24*60*60*1000L);
  
  /** Simplify a Quantity schedule by producing the smallest
   * schedule which has the same quantity-value curve.  That is,
   * no temporally-overlapping elements and no abutting elements
   * with the same value.
   **/
  public static Schedule simplifySchedule(Schedule s) {
    //return combineLikeQuantityElements(computeNonOverlappingSchedule(s));
    return computeNonOverlappingSchedule(s);
  }

  /**
   * Return non-overlapping schedules from thescheduleelements.
   * Assumes that all schedule elements are ScheduleElementWithValue
   * Sums all ScheduleElementWithValue on a per day basis.
   * @param aSchedule
   * @return Schedule  A new Schedule object with non-overlapping (time) 
   * ScheduleElementWithValue
   * @see org.cougaar.domain.planning.ldm.plan.Schedule
   * @see org.cougaar.domain.planning.ldm.plan.ScheduleElementWithValue
   * @throws IllegalArgumentException
   */
  public static Schedule computeNonOverlappingSchedule(Schedule aSchedule) {
    final Vector scheduleElements = new Vector();
    final boolean isQuantity = aSchedule.getScheduleElementType().equals(ScheduleElementType.QUANTITY);

    class MyThunk implements Thunk {
      ScheduleElement pending = null;

      /** a list of pushed back scheduleElements ordered by decreasing start time **/
      private ArrayList pbl = new ArrayList();

      /** insert se at the right place in the pushback list **/
      private void pushback(ScheduleElement se) {
        int l = pbl.size();
        long s0 = se.getStartTime();
        for (int i = 0; i<l; i++) {
          if (s0 > ((ScheduleElement)pbl.get(i)).getStartTime()) {
            pbl.add(i, se);
            return;
          }
        }
        pbl.add(se);
      }
      
      public void apply(Object o) {
        ScheduleElement se = (ScheduleElement) o;
        long s0 = se.getStartTime();
        int l;
        while ((l = pbl.size()) != 0) {
          int l1 = l-1;
          ScheduleElement last = (ScheduleElement) pbl.get(l1);
          if (last.getStartTime() <= s0) {
            pbl.remove(l1);
            consumeOne(last);
          } else {
            break;
          }
        }
        consumeOne(se);
      }

      public void finish() {
        int l;
        while ((l = pbl.size()) != 0) {
          int l1 = l-1;
          ScheduleElement last = (ScheduleElement) pbl.get(l1);
          pbl.remove(l1);
          consumeOne(last);
        }
        if (pending != null) scheduleElements.add(pending);
      }

      private void consumeOne(ScheduleElement se) {
        if (pending == null) {
          pending = se;         // nothing pending, so just watch for next iteration
        } else {
          if (pending.overlapSchedule(se)) {
            long p0 = pending.getStartTime();
            long p1 = pending.getEndTime();
            long s0 = se.getStartTime();
            long s1 = se.getEndTime();

            // create an initial span before the overlapping span
            if (p0<s0) {
              scheduleElements.add(createElement(p0, s0, pending, null));
            } 
            // compute the tail
            long lesser;
            if (p1 < s1) {
              lesser = p1;
              pushback(createElement(p1, s1, se, null));
            } else if (p1 > s1) {
              lesser = s1;
              pushback(createElement(s1, p1, pending, null));
            } else {
              lesser = s1;
            }

            // add the overlap
            pending = createElement(s0, lesser, pending, se);
          } else {
            scheduleElements.add(pending);
            pending = se;
          }
        }
      }
      
      /** e1 must be non-null **/
      private ScheduleElement createElement(long start, long end, ScheduleElement e1, ScheduleElement e2) {
        
        double acc = ((ScheduleElementWithValue) e1).getValue();
        if (e2 != null)
          acc += ((ScheduleElementWithValue) e2).getValue();
        return ((ScheduleElementWithValue)e1).newElement(start,end,acc);
      }
    };
    MyThunk thunk = new MyThunk();

    aSchedule.applyThunkToScheduleElements(thunk);
    thunk.finish();            // finish up the pushedback elements

    // create a new schedule with the new elements
    ScheduleImpl newsched = new ScheduleImpl();
    newsched.setScheduleType(aSchedule.getScheduleType());
    if (isQuantity) {
      newsched.setScheduleElementType(ScheduleElementType.QUANTITY);
    } else {
      newsched.setScheduleElementType(ScheduleElementType.RATE);
    }

    newsched.setScheduleElements(scheduleElements);
    
    return newsched;
  }


  /**
   * Returns a schedule that creates ScheduleElementWithValue that span multiple
   * days if there are Like ScheduleElementWithValues during that time period.
   * This is more efficient than having a scheduleelement for each day.
   * This method expects a Schedule containing ONLY ScheduleElementWithValues
   * @param aSchedule
   * @return Schedule
   * @see org.cougaar.domain.planning.ldm.plan.Schedule
   * @see org.cougaar.domain.planning.ldm.plan.ScheduleElementWithValue
   * @throws IllegalArgumentException
   **/
  public static Schedule combineLikeQuantityElements(Schedule aSchedule) {
    if (! aSchedule.getScheduleElementType().equals(ScheduleElementType.QUANTITY)) {
      throw new IllegalArgumentException("ScheduleUtilities.combineLikeQuantityElements expects " +
                "a Schedule with ScheduleElementWithValues!");
    }
    double currentQuantity = 0;
    double quantity = 0;
    long startTime = 0;
    long endTime = 0;
    Vector minimalScheduleElements = new Vector();
    Enumeration allelements = aSchedule.getAllScheduleElements();
    Vector scheduleElements = new Vector();
    while (allelements.hasMoreElements()) {
      ScheduleElement se = (ScheduleElement) allelements.nextElement();
      scheduleElements.add(se);
    }
        
    if (scheduleElements.size() > 0) {
      ScheduleElementWithValue s = (ScheduleElementWithValue)scheduleElements.elementAt(0);
      startTime = s.getStartTime();
      endTime = s.getEndTime();
      currentQuantity = s.getValue();
    
      for (int i = 1; i < scheduleElements.size(); i++) {
        s = (ScheduleElementWithValue)scheduleElements.elementAt(i);
        if (s.getStartTime() > (endTime+1000) ||
            s.getValue() != currentQuantity) {
          minimalScheduleElements.add(s.newElement(startTime, endTime, currentQuantity));
          currentQuantity = s.getValue();
          startTime = s.getStartTime();
        }
        endTime = s.getEndTime();
      }
      //get the last range element
      if (startTime != 0 && (endTime-startTime)>1000L) {
        minimalScheduleElements.add(s.newElement(startTime, endTime, currentQuantity));
      }
    } 

    // create a new schedule to return
    ScheduleImpl newsched = new ScheduleImpl();
    newsched.setScheduleType(aSchedule.getScheduleType());
    newsched.setScheduleElementType(ScheduleElementType.QUANTITY);
    newsched.setScheduleElements(minimalScheduleElements);
    
    return newsched;
  }
  
  /** 
   * Add the quantities of the scheduleelements in the set.  
   * Must be a set of ScheduleElementWithValues or RateScheduleElement
   * @param aSet  Called with all the schedule elements for a day.
   * @return double
   * @throws IllegalArgumentException
   **/
  public static double sumElements(Collection aSet) {
    if (aSet == null) return 0.0;
    double quantity = 0;
    for (Iterator i = aSet.iterator(); i.hasNext(); ) {
      ScheduleElementWithValue s = (ScheduleElementWithValue)i.next();
      quantity += s.getValue();
    }
    return quantity;
  }

  //
  // thunk utilities
  //

  /** Utility to select ScheduleElements which overlap with a specific time **/
  public void selectElementsAtTime(Schedule s, final Thunk t, final long d) {
    s.applyThunkToScheduleElements(new Thunk() {
        public void apply(Object o) {
          ScheduleElement se = (ScheduleElement) o;
          if (d>=se.getStartTime() && d<se.getEndTime())
            t.apply(o);
        }
      });
  }

  /** Utility to select ScheduleElements which overlap with a time span **/
  public void selectElementsOverlappingSpan(Schedule s, final Thunk t, final long startt, final long endt) {
    s.applyThunkToScheduleElements(new Thunk() {
        public void apply(Object o) {
          ScheduleElement se = (ScheduleElement) o;
          if (se.getStartTime()<endt && se.getEndTime()>startt)
            t.apply(o);
        }
      });
  }
  
  /** Utility to select ScheduleElements which overlap with a time span **/
  public void selectElementsOverlappingSpan(Schedule s, final Thunk t, ScheduleElement span) {
    selectElementsOverlappingSpan(s,t,span.getStartTime(),span.getEndTime());
  }

  /** Utility to select ScheduleElements which enclose (or equal) a time span **/
  public void selectElementsEnclosingSpan(Schedule s, final Thunk t, final long startt, final long endt) {
    s.applyThunkToScheduleElements(new Thunk() {
        public void apply(Object o) {
          ScheduleElement se = (ScheduleElement) o;
          if (se.getStartTime()>=startt && se.getEndTime()<=endt)
            t.apply(o);
        }
      });
  }

  /** Utility to select ScheduleElements which enclose (or equal) a time span **/    
  public void selectElementsEnclosingSpan(Schedule s, final Thunk t, ScheduleElement span) {
    selectElementsEnclosingSpan(s,t,span.getStartTime(),span.getEndTime());
  }



  //////////////// For UIPlugIns and ScheduleImpl internals ////////////

  /** combine two quantity schedules. **/
  public static final Schedule addSchedules(Schedule aSchedule, 
                                            Schedule bSchedule) {
    Schedule tmp = new ScheduleImpl(aSchedule);
    tmp.addAll(bSchedule);
    return simplifySchedule(tmp);
  }

  /** Subtract out the bSchedule from the aSchedule **/
  public static final Schedule subtractSchedules(Schedule aSchedule, 
                                                 Schedule bSchedule) {
    Schedule tmp = new ScheduleImpl(aSchedule);
    for (Iterator i = bSchedule.iterator(); i.hasNext(); ) {
      ScheduleElementWithValue e = (ScheduleElementWithValue) i.next();
      ScheduleElementWithValue n = e.newElement(e.getStartTime(),
                                                e.getEndTime(),
                                                -e.getValue());
      tmp.add(n);
    }
    return simplifySchedule(tmp);
  }
}
