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
package org.cougaar.planning.ldm.asset;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.SimpleBeanInfo;
import java.beans.PropertyDescriptor;

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.NewTimeSpan;
import org.cougaar.util.NonOverlappingTimeSpanSet;
import org.cougaar.util.TimeSpan;

public class PropertyGroupSchedule extends NonOverlappingTimeSpanSet 
  implements BeanInfo, Cloneable {
 
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

    if (schedule.getPGClass() != null) {
      initializePGClass(schedule.getPGClass());      
    }

    if (schedule.getDefault() != null) {
      setDefault(schedule.getDefault());
    }
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

  public void initializePGClass(PropertyGroup propertyGroup) {
    initializePGClass(propertyGroup.getPrimaryClass());
  }
                                                
  public Class getPGClass() {
    return myPropertyGroupClass;
  }

  public void setDefault(TimePhasedPropertyGroup defaultPG) {
    if (!validClass(defaultPG.getPrimaryClass())) {
      System.out.println("PGClass: " + getPGClass() + " default arg " + 
                         defaultPG.getPrimaryClass());
      throw new ClassCastException();
    }

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
    if (!validClass(((PropertyGroup) o).getPrimaryClass())) {
      throw new ClassCastException();
    }
    
    myTiling = null;

    TimeSpan timeSpan = (TimeSpan) o;
    if ((timeSpan.getStartTime() == TimeSpan.MIN_VALUE) && 
        (timeSpan.getEndTime() == TimeSpan.MAX_VALUE)) {
      setDefault((TimePhasedPropertyGroup) o);

      return true;
    } else {
      return super.add(o);
    }
  }

  public boolean remove(Object o) {
    if (!validClass(((PropertyGroup) o).getPrimaryClass())) {
      throw new ClassCastException();
    }

    myTiling = null;

    if (o.equals(getDefault())) {
        clearDefault();
        return true;
    } else {
      return super.remove(o);
    }
      
  }

  public NonOverlappingTimeSpanSet getTiling(long startTime, long endTime) {
    if (myDefault == null) {
      return new NonOverlappingTimeSpanSet(intersectingSet(startTime, endTime));
    }

    if (myTiling == null) {
      myTiling = fill((NewTimeSpan) myDefault.copy());
    }

    Exception e = new RuntimeException("Tiling count: " + myTiling.size());
    e.printStackTrace();

    return new NonOverlappingTimeSpanSet(myTiling.intersectingSet(startTime, 
                                                                  endTime));
  }

  public NonOverlappingTimeSpanSet getTiling(TimeSpan timeSpan) {
    return getTiling(timeSpan.getStartTime(), timeSpan.getEndTime());
  }
      

  public NonOverlappingTimeSpanSet getTiling() {
    return getTiling(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE);
  }

  public Object[] getTilingAsArray() {
    return getTiling(TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE).toArray();
  }

  public Object clone() {
    /*
    RuntimeException e = new RuntimeException();
    e.printStackTrace();
    */
    PropertyGroupSchedule schedule = new PropertyGroupSchedule(this);
    schedule.lockPGs();

    return schedule;
  }

  public void lockPGs(Object key) {
    if (getDefault() != null) {
      setDefault((TimePhasedPropertyGroup)getDefault().lock(key));
    }

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
    if (getDefault() != null) {
      setDefault((TimePhasedPropertyGroup)getDefault().unlock(key));
    }

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

  /** @return the method name on an asset to retrieve the PG **/
  public String getAssetGetMethod() {
    try {
      return  ((PropertyGroup) (getPGClass().newInstance())).getAssetGetMethod() + 
     "Schedule";
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
    
  }

  /** @return the method name on an asset to set the PG **/
  public String getAssetSetMethod() {
    try {
      return  ((PropertyGroup)(getPGClass().newInstance())).getAssetSetMethod() + 
     "Schedule";
    } catch (Exception e) {
      e.printStackTrace();
      return "";
    }
  }
  
  private static PropertyDescriptor properties[];

  static {
    try {
      properties = new PropertyDescriptor[3];

      properties[0]= new PropertyDescriptor("property_group_class", PropertyGroupSchedule.class, "getPGClass", null);
      properties[1]= new PropertyDescriptor("default", PropertyGroupSchedule.class, "getDefault", null);
      properties[2]= new PropertyDescriptor("schedule", PropertyGroupSchedule.class, "toArray", null);
    } catch (Exception e) { System.err.println("Caught: "+e); e.printStackTrace(); }
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }

  public Class getIntrospectionClass() {
    return PropertyGroupSchedule.class;
  }

    /**
     * Deny knowledge about the class and customizer of the bean.
     * You can override this if you wish to provide explicit info.
     */
    public BeanDescriptor getBeanDescriptor() {
	return null;
    }

    /**
     * Deny knowledge of a default property. You can override this
     * if you wish to define a default property for the bean.
     */
    public int getDefaultPropertyIndex() {
	return -1;
    }

    /**
     * Deny knowledge of event sets. You can override this
     * if you wish to provide explicit event set info.
     */
    public EventSetDescriptor[] getEventSetDescriptors() {
	return null;
    }

    /**
     * Deny knowledge of a default event. You can override this
     * if you wish to define a default event for the bean.
     */
    public int getDefaultEventIndex() {
	return -1;
    }

    /**
     * Deny knowledge of methods. You can override this
     * if you wish to provide explicit method info.
     */
    public MethodDescriptor[] getMethodDescriptors() {
	return null;
    }

    /**
     * Claim there are no other relevant BeanInfo objects.  You
     * may override this if you want to (for example) return a
     * BeanInfo for a base class.
     */
    public BeanInfo[] getAdditionalBeanInfo() {
	return null;
    }

    /**
     * Claim there are no icons available.  You can override
     * this if you want to provide icons for your bean.
     */
    public java.awt.Image getIcon(int iconKind) {
	return null;
    }

    /**
     * This is a utility method to help in loading icon images.
     * It takes the name of a resource file associated with the
     * current object's class file and loads an image object
     * from that file.  Typically images will be GIFs.
     * <p>
     * @param resourceName  A pathname relative to the directory
     *		holding the class file of the current class.  For example,
     *		"wombat.gif".
     * @return  an image object.  May be null if the load failed.
     */
    public java.awt.Image loadImage(final String resourceName) {
	try {
	    final Class c = getClass();
	    java.awt.image.ImageProducer ip = (java.awt.image.ImageProducer)
		java.security.AccessController.doPrivileged(
		new java.security.PrivilegedAction() {
		    public Object run() {
			java.net.URL url;
			if ((url = c.getResource(resourceName)) == null) {
			    return null;
			} else {
			    try {
				return url.getContent();
			    } catch (java.io.IOException ioe) {
				return null;
			    }
			}
		    }
	    });

	    if (ip == null)
		return null;
	    java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
	    return tk.createImage(ip);
	} catch (Exception ex) {
	    return null;
	}
    }

}














