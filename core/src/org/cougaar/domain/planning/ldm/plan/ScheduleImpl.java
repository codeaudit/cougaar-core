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

import org.cougaar.util.*;
import java.util.*;

/**
 * A Schedule is an encapsulation of spatio-temporal relationships.
 * It has a collection of ScheduleElements.
 */

public class ScheduleImpl 
  extends SynchronizedTimeSpanSet
  implements Schedule, NewSchedule
{
  protected String scheduleType = ScheduleType.OTHER;
  protected Class scheduleElementType = ScheduleElement.class;
                
  /** Construct an empty schedule **/
  public ScheduleImpl () {
    // default scheduleType to Other since quantity and quantityrange schedule
    // elements are probably the only schedules using meaningful scheuduletypes
    // for 1999
    scheduleType = ScheduleType.OTHER;
  }
        
  /** Construct a schedule which has the same elements as the specified
   * collection.  If the specified collection needs to be sorted, it will
   * be.
   **/
  public ScheduleImpl(Collection c) {
    super(c.size());
    
    if (c instanceof Schedule) {
      Schedule s = (Schedule)c;
      scheduleType = s.getScheduleType();
      scheduleElementType = s.getScheduleElementType();
      unsafeUpdate(c);
    } else {
      scheduleType = ScheduleType.OTHER;
      scheduleElementType = ScheduleElementType.MIXED;
      addAll(c);
    }

  }

  public String getScheduleType() {
    return scheduleType;
  }
  
  public Class getScheduleElementType() {
    return scheduleElementType;
  }
        
  public synchronized Date getStartDate() {
    TimeSpan ts = (TimeSpan) first();
    if (ts == null) {
      throw new IndexOutOfBoundsException("Called getStartDate on an empty schedule");
    }
    return new Date(ts.getStartTime());
  }

  public synchronized long getStartTime() {
    TimeSpan ts = (TimeSpan) first();
    return (ts == null)? MIN_VALUE : ts.getStartTime();
  }

  public synchronized Date getEndDate() {
    TimeSpan ts = (TimeSpan) last();
    if (ts == null) {
      throw new IndexOutOfBoundsException("Called getEndDate on an empty schedule");
    }
    return new Date(getEndTime());
  }

  public synchronized long getEndTime() {
    int l = size;
    if (l == 0) return MAX_VALUE;

    long max = MIN_VALUE;
    for (int i = 0; i<l; i++) {
      ScheduleElement se = (ScheduleElement) elementData[i];
      long end = se.getEndTime();
      if (end > max) max = end;
    }
    return max;
  }

  /** get an enumeration over a copy of all of the schedule elements of this 
   * schedule.
   * Note that this is a copy, changes to the underlying schedule will not be 
   * reflected in the Enumeration.
   * @return Enumeration{ScheduleElement}
   */
  public synchronized Enumeration getAllScheduleElements() {
    ArrayList copy = new ArrayList(this);
    return new Enumerator(copy);
  }
   
  public synchronized Collection filter(UnaryPredicate predicate) {
    return Filters.filter(protectedIterator(), predicate);
  }

  
  /** get a colleciton of schedule elements that include this date.
   * Note that the schedule element can have a start or end date
   * that equals the given date or the date may fall in the time span
   * of a schedule element.
   * @return OrderedSet
   */
  public synchronized Collection getScheduleElementsWithDate(Date aDate) {
    return getScheduleElementsWithTime(aDate.getTime());
  }

  public synchronized Collection getScheduleElementsWithTime(final long aTime) {
    return intersectingSet(aTime);
  }
        
  public synchronized void applyThunkToScheduleElements(Thunk t) {
    Collectors.apply(t, this);
  }


  /** get a sorted Collection of schedule elements that have dates in the
   * given range of dates.  Note that these schedule elements may
   * or may not be fully bound by the date range - they may overlap.
   * @return OrderedSet
   */
  public synchronized Collection getOverlappingScheduleElements(Date startDate, Date endDate){
    return getOverlappingScheduleElements(startDate.getTime(), endDate.getTime());
  }

  public synchronized Collection getOverlappingScheduleElements(final long startTime, 
                                                                final long endTime)
  {
    return intersectingSet(startTime, endTime);
  }

  /** get a Collection of schedule elements that are fully bound
   * or encapsulated by a date range.
   * @return OrderedSet
   */
  public synchronized Collection getEncapsulatedScheduleElements(Date startDate, Date endDate) {
    return getEncapsulatedScheduleElements(startDate.getTime(), endDate.getTime());
  } 
        
  public synchronized Collection getEncapsulatedScheduleElements(final long startTime,
                                                                 final long endTime)
  {
    return encapsulatedSet(startTime, endTime);
  }

  /** add a single schedule element to the already existing Schedule.
   * @param aScheduleElement
   */
  public synchronized void addScheduleElement(ScheduleElement aScheduleElement) {
    add(aScheduleElement);
  }
        
  public synchronized void removeScheduleElement(ScheduleElement aScheduleElement) {
    remove(aScheduleElement);
  }

  public synchronized void clearScheduleElements() {
    clear();
  }

  /** set a single schedule element - used for a simple schedule
   * container will be cleared before it is added to ensure that 
   * there is only one schedule element.
   * @param aScheduleElement
   **/
  public synchronized void setScheduleElement(ScheduleElement aScheduleElement) {
    clear();
    add(aScheduleElement);
  }
  
  /** Return the Collection.   
   * This is now a noop.
   **/
  public Collection UnderlyingContainer() {
    return this;
  }


  public boolean isAppropriateScheduleElement(Object o) {
    return (scheduleElementType.isAssignableFrom(o.getClass()));
  }
        
  /** Set the schedule elements for this schedule.
   * Note this method assumes that you are adding things to 
   * an empty container, hence it clears the container of old
   * schedule elements before setting the new ones.
   */
  public synchronized void setScheduleElements(Collection collection) {
    clear();
    if (collection == null) return;    // setting it to null clears it

    addAll(collection);
  }

  // Over write ArrayList methods

  /** add object to Schedule. Verifies that object matches specifed
   * ScheduleElement type.
   * @param o Object to add to Schedule
   * @return boolean true if successful, else false
   */
  public synchronized boolean add(Object o) {
    if (!isAppropriateScheduleElement(o)) {
      ClassCastException cce = new ClassCastException("ScheduleImpl.add(Object o): o - " + o + " does not match schedule element type - " + getScheduleElementType());
      cce.printStackTrace();
      return false;
    }
    
    return super.add(o);
  }
      
  /** returns Iterator over a copy of the Schedule. Prints a warning and
   *  dumps a stack trace.
   *  Use filter() to get an copy which can be iterated over without 
   *  the warning.
   *  @return Iterator over a copy
   */
  public synchronized Iterator iterator() {
    UnsupportedOperationException uae = 
      new UnsupportedOperationException("ScheduleImpl.iterator()." +
                                        " Returning an iterator over a copy of this Schedule");
    uae.printStackTrace();
    
    ArrayList copy = new ArrayList(this);
    return copy.iterator();
  }

  /** listIterator - Unsupported
   *  @throw UnsupportedOperationException
   */
  public synchronized ListIterator listIterator() {
    throw new UnsupportedOperationException("ScheduleImpl.listIterator() is not supported.");
  }

  /** listIterator - Unsupported
   *  @throw UnsupportedOperationException
   */
  public synchronized ListIterator listIterator(int index) {
    throw new UnsupportedOperationException("ScheduleImpl.listIterator(int index) is not supported.");
  }

  /** returns a subset from a copy of the Schedule.  Prints a warning and
   *  dumps a stack trace. Subset made from a copy of the Schedule so that 
   *  the Schedule continues to Synchronization safe.
   *  Use filter() to get an copy which can be iterated over without 
   *  the warning.
   *  @return Iterator over a copy
   */
  public synchronized List subList(int fromIndex, int toIndex) {
    UnsupportedOperationException uae = 
      new UnsupportedOperationException("ScheduleImpl.subList(int fromIndex, int toIndex)" +
                                        " Returning an subList over a copy of this Schedule");
    uae.printStackTrace();

    ArrayList copy = new ArrayList(this);
    return copy.subList(fromIndex, toIndex);
  }

  public synchronized void setScheduleElements(Enumeration someScheduleElements) {
    //clear the container
    clear();
    //unpack the enumeration
    while (someScheduleElements.hasMoreElements()) {
      Object o = someScheduleElements.nextElement();
      if (o instanceof ScheduleElement) {
        add((ScheduleElement)o);
      } else {
        throw new IllegalArgumentException("Schedule.setScheduleElements(enum e) expects that the objects contained in the enumeration are of type ScheduleElement");
      }
    }
  }
        
  /* setScheduleType - type can only be set for empty schedule.
   */
  public void setScheduleType(String type) {
    if (!isEmpty()) {
      throw new ClassCastException("Can not set ScheduleType for non-empty schedule");
    }
    scheduleType = type;
  }
  
  public void setScheduleElementType(Class setype) {
    if (!ScheduleElement.class.isAssignableFrom(setype)) {
      throw new ClassCastException("ScheduleElement class is not assignable from " + setype);
    } else if (!isEmpty() &&
               !scheduleElementType.isAssignableFrom(setype)) {
      throw new ClassCastException(setype + 
                                   " is not assignable from current ScheduleElement type " + 
                                   scheduleElementType);
    }
    scheduleElementType = setype;
  }
                        
  public String toString() {
    String tstring = "?";
    String setstring = "?";
    if (scheduleType!=null) 
      tstring = scheduleType;
    if (scheduleElementType != null) 
      setstring = scheduleElementType.toString();
    return "\n<Schedule "+tstring+"/"+setstring+" "+super.toString()+">";
  }

        
  /* methods returned by ScheduleImplBeanInfo */

  public synchronized ScheduleElement[] getScheduleElements() {
    ScheduleElement s[] = new ScheduleElement[size()];
    return (ScheduleElement[])toArray(s);
  }

  public synchronized ScheduleElement getScheduleElement(int i) {
    return (ScheduleElement)elementData[i];
  }

  protected Iterator protectedIterator() {
    return super.iterator();
  }


  public static void main(String []args) {
    Vector vector = new Vector();
    vector.add(new LocationScheduleElementImpl());

    ScheduleImpl lsSchedule = new ScheduleImpl();
    lsSchedule.setScheduleElementType(ScheduleElementType.LOCATION);
    lsSchedule.add(new LocationScheduleElementImpl());


    ScheduleImpl schedule = new ScheduleImpl(vector);
    System.out.println(schedule);

    //Schedule schedule = new ScheduleImpl(lsSchedule);
    schedule = new ScheduleImpl(lsSchedule);
    System.out.println(schedule);

    schedule.setScheduleElements(vector);
    schedule.addAll(1, vector);
  }
}






