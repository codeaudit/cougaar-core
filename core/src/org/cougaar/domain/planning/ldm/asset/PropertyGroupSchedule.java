/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.asset;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.NewTimeSpan;
import org.cougaar.util.NonOverlappingTimeSpanSet;
import org.cougaar.util.TimeSpan;

public class PropertyGroupSchedule extends NonOverlappingTimeSpanSet 
  implements Cloneable {

  private Class myPropertyGroupClass = null;
  private TimePhasedPropertyGroup myDefault = null;

  private NonOverlappingTimeSpanSet myTiling = null;

  // constructors
  public PropertyGroupSchedule() {
    super();
  }

  // constructors
  public PropertyGroupSchedule(TimePhasedPropertyGroup defaultPG) {
    super();

    setDefault(defaultPG);
  }


  public PropertyGroupSchedule(PropertyGroupSchedule schedule) {
    super(schedule);

    initializePGClass(schedule.getPGClass());      
    setDefault(schedule.getDefault());
  }

  public PropertyGroupSchedule(Collection c) {
    super(c.size());
      
    // insert them carefully.
    for (Iterator i = c.iterator(); i.hasNext();) {
      add(i.next());
    }
  }

  public void initializePGClass(Class tppgClass) {
    if (myPropertyGroupClass != null) {
      // Can only initialize class once
      throw new IllegalArgumentException();
    } else if (!TimePhasedPropertyGroup.class.isAssignableFrom(tppgClass)) {
      //Must be an extension of PropertyGroup
      throw new ClassCastException();
    }
    
    myPropertyGroupClass = tppgClass;
  }
                                                
  public Class getPGClass() {
    return myPropertyGroupClass;
  }

  public void setDefault(TimePhasedPropertyGroup defaultPG) {
    if (!validClass(defaultPG.getClass())) {
      throw new ClassCastException();
    }
    // Default must be bot/eot

    myDefault = defaultPG;
    
    myTiling = null;
  }

  public TimePhasedPropertyGroup getDefault() {
    return myDefault;
  }

  public void clearDefault() {
    myDefault = null;

    myTiling = null;
  }

  public boolean add(Object o) {
    if (!validClass(o.getClass())) {
      throw new ClassCastException();
    }
    
    myTiling = null;

    return super.add(o);
  }

  public boolean remove(Object o) {
    if (!validClass(o.getClass())) {
      throw new ClassCastException();
    }

    myTiling = null;

    return super.remove(o);
  }

  public NonOverlappingTimeSpanSet getTiling(long startTime, long endTime) {
    if (myDefault == null) {
      return new NonOverlappingTimeSpanSet(intersectingSet(startTime, endTime));
    }

    if (myTiling == null) {
      myTiling = fill((NewTimeSpan)myDefault);
    }

    return new NonOverlappingTimeSpanSet(myTiling.intersectingSet(startTime, 
                                                                  endTime));
  }

  public NonOverlappingTimeSpanSet getTiling(TimeSpan timeSpan) {
    return getTiling(timeSpan.getStartTime(), timeSpan.getEndTime());
  }
      
  public Object clone() {
    return new PropertyGroupSchedule(this);
  }

  public void lockPGs(Object key) {
    setDefault((TimePhasedPropertyGroup)getDefault().lock(key));
    
    Collection set = intersectingSet(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    clear();
    for (Iterator i = set.iterator(); i.hasNext();) {
      add(((PropertyGroup)i.next()).lock(key));
    }
  }

  public void lockPGs() {
    lockPGs(null);
  }

  public void unlockPGs(Object key) throws IllegalAccessException {
    setDefault((TimePhasedPropertyGroup)getDefault().unlock(key));
    
    Collection set = intersectingSet(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
    clear();
    for (Iterator i = set.iterator(); i.hasNext();) {
      add(((PropertyGroup)i.next()).unlock(key));
    }
  }
    
  protected boolean validClass(Class tppgClass) {
    if (myPropertyGroupClass == null) {
      try {
        initializePGClass(tppgClass);
      } catch (ClassCastException e) {
        return false;
      }
    }

    return tppgClass.equals(myPropertyGroupClass);
  }

//    public static void main(String arg[]) {

//      PropertyGroupSchedule schedule = new PropertyGroupSchedule();

//      ItemIdentificationPGImpl defaultPG = new ItemIdentificationPGImpl();
//      defaultPG.setTimeSpan(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
//      defaultPG.setItemIdentification("DEFAULT");
//      defaultPG.setNomenclature("DEFAULT");
//      defaultPG.setAlternateItemIdentification("DEFAULT");

//      ItemIdentificationPGImpl item1 = new ItemIdentificationPGImpl();
//      item1.setTimeSpan(-5, 20);
//      item1.setItemIdentification("ITEM1");
//      item1.setNomenclature("ITEM1");
//      item1.setAlternateItemIdentification("ITEM1");

//      ItemIdentificationPGImpl item2 = new ItemIdentificationPGImpl();
//      item2.setTimeSpan(30, 90);
//      item2.setItemIdentification("ITEM2");
//      item2.setNomenclature("ITEM2");
//      item2.setAlternateItemIdentification("ITEM2");

//      schedule.setDefault(defaultPG);
//      schedule.add(item1);
//      schedule.add(item2);

//      System.out.println("Initial");
//      Iterator iterator = schedule.iterator();
//      while(iterator.hasNext()) {
//        ItemIdentificationPG pg = (ItemIdentificationPG)iterator.next();
//        System.out.println("\t" + pg.getItemIdentification() + " " + 
//                           pg.getStartTime() + " " + 
//                           pg.getEndTime());
//      }
    
//      System.out.println("Filled");
//      iterator = 
//        schedule.getTiling(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE).iterator();
//      while(iterator.hasNext()) {
//        ItemIdentificationPG pg = (ItemIdentificationPG)iterator.next();
//        System.out.println("\t" + pg.getItemIdentification() + " " + 
//                           pg.getStartTime() + " " + 
//                           pg.getEndTime());
//      }

//    }
}








