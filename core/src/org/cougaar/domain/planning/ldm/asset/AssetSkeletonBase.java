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

/** Basic functionality for AssetSkeletons
 * Implements otherProperties
 **/

package org.cougaar.domain.planning.ldm.asset;

import org.cougaar.domain.planning.ldm.*;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.planning.ldm.plan.*;
import java.util.*;
import java.io.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import java.beans.SimpleBeanInfo;

public abstract class AssetSkeletonBase
  extends SimpleBeanInfo
  implements Serializable, Cloneable 
{

  /** additional properties searched by default get*PG methods.
   *  Includes PropertyGroups and PropertyGroupSchedules
   **/
  private ArrayList otherProperties = null;
  
  protected boolean hasOtherTimePhasedProperties = false;

  public boolean hasOtherTimePhasedProperties() {
    return hasOtherTimePhasedProperties;
  }

  synchronized ArrayList copyOtherProperties() { 
    if (otherProperties==null)
      return null;
    else
      return (ArrayList) otherProperties.clone();
  }
        
  private synchronized final ArrayList force() {
    if (otherProperties==null)
      otherProperties=new ArrayList(1);
    return otherProperties;
  }

  protected AssetSkeletonBase() {}

  protected AssetSkeletonBase(AssetSkeletonBase prototype) {
    otherProperties = prototype.copyOtherProperties();
    hasOtherTimePhasedProperties = prototype.hasOtherTimePhasedProperties();
  }

  protected void fillAllPropertyGroups(Collection v) {
    v.addAll(force());
  }

  protected void fillAllPropertyGroups(Collection v, long time) {
    if (!hasOtherTimePhasedProperties()) {
      fillAllPropertyGroups(v);
      return;
    }

    for (Iterator i = force().iterator(); i.hasNext();) {
      Object o = i.next();
      if (o instanceof PropertyGroupSchedule) {
        PropertyGroup pg = 
          (PropertyGroup)((PropertyGroupSchedule)o).intersects(time);
        if (pg != null) {
          v.add(pg);
        }
      } else {
        v.add(o);
      }
    }
  }

  /** @return the set of additional properties (includes PropertyGroups and
   * PropertyGroupSchedules) - not synchronized!**/
  public synchronized Enumeration getOtherProperties() {
    if (otherProperties == null || otherProperties.size()==0)
      return Empty.enumeration;
    else
      return new Enumerator(otherProperties);
  }

  /** replace the existing set of other properties (PropertyGroups and
   *  PropertyGroupSchedules)
   **/
  protected synchronized void setOtherProperties(Collection newProps) {
    synchronized (otherProperties) {
      if (otherProperties != null) {
        otherProperties.clear();
        hasOtherTimePhasedProperties = false;
      } else {
        force();
      }

      otherProperties.addAll(newProps);

      // Check for time phased properties
      for (Iterator i = newProps.iterator(); i.hasNext();) {
        Object o = i.next();
        if (o instanceof PropertyGroupSchedule) {
          hasOtherTimePhasedProperties = true;
          break;
        }
      }
    }    
  }

  /** Add a PropertyGroup to the set of properties
   *  @param prop PropertyGroup to add
   **/
  public void addOtherPropertyGroup(PropertyGroup prop) {
    setLocalPG(prop.getPrimaryClass(), prop);
  }

  /** Add a PropertyGroupSchedule to the set of properties.
   *  @param pgs PropertyGroupSchedule to add
   **/
  public void addOtherPropertyGroupSchedule(PropertyGroupSchedule pgs) {
    setLocalPGSchedule(pgs);
  }

  /** Replace a PropertyGroup in the set of properties. 
   *  @param prop PropertyGroup to replace 
   **/
  public void replaceOtherPropertyGroup(PropertyGroup prop) {
    setLocalPG(prop.getPrimaryClass(), prop);
  }

  /** Replace a PropertyGroupSchedule in the set properties.
   *  @oaram schedule PropertyGroupSchedule to replace.
   **/
  public void replaceOtherPropertyGroupSchedule(PropertyGroupSchedule schedule) {
    setLocalPGSchedule(schedule);
  }

  /** Removes the PropertyGroup matching the class passed in as an argument 
   * from the set of properties.
   * Note: this implementation assumes that the set of properties
   * holds one and only one instance of a given class.
   * @param c Class to match.
   * @return PropertyGroup Return the property instance that was removed; 
   * otherwise, return null.
   **/ 
  public PropertyGroup removeOtherPropertyGroup(Class c) {
    return removeLocalPG(c);
  }


  /** Removes the PropertyGroup passed in as an argument from the set of 
   * properties.
   * @param pg PropertyGroup to remove
   * @return PropertyGroup Return the property instance that was removed; 
   * otherwise, return null.
   **/ 
  public PropertyGroup removeOtherPropertyGroup(PropertyGroup pg) {
    return removeLocalPG(pg);
  }

  /** Removes the PropertyGroupSchedule whose PGClass matched the class passed
   * in as an argument from the set of properties. 
   * Note: this implementation assumes that set of additional properties holds
   * one and only one instance of this PropertyGroupSchedule
   * @param c Class to match.
   * @return PropertyGroupSchedule Return the PropertyGroupSchedule that was 
   * removed; otherwise, return null.
   **/ 
  public PropertyGroupSchedule removeOtherPropertyGroupSchedule(Class c) {
    return removeLocalPGSchedule(c);
  }

  /** Removes the instance matching the PropertyGroupSchedule passed in as an
   * argument 
   * Note: this implementation assumes that set of additional properties holds
   * one and only one instance of a given PropertyGroupSchedule
   * @param pgs PropertyGroupSchedule to remove. Match based on the schedule's
   * PGClass.
   * @return PropertyGroupSchedule Return the instance that was removed; 
   * otherwise, return null.
   **/ 
  public PropertyGroupSchedule removeOtherPropertyGroupSchedule(PropertyGroupSchedule pgs) {
    return removeLocalPGSchedule(pgs.getPGClass());
  }

  /** return the PropertyGroupSchedule associated with the specified class.
   * @param
   **/
  public synchronized PropertyGroupSchedule searchForPropertyGroupSchedule(Class c) {
    if (!hasOtherTimePhasedProperties) {
      return null;    
    }

    // Use time phased method
    if (!TimePhasedPropertyGroup.class.isAssignableFrom(c)) {
      return null;
    }

    int index = findLocalPGScheduleIndex(c);

    if (index >= 0){ 
      return (PropertyGroupSchedule) force().get(index);
    } else {
      return null;
    }
  }


  /** Convenient equivalent to searchForPropertyGroupSchedule(pg.getClass()) **/
  public final PropertyGroupSchedule searchForPropertyGroupSchedule(PropertyGroup pg) {
    return searchForPropertyGroupSchedule(pg.getPrimaryClass());
  }


  //
  // new PG resolution support
  //
  
  protected final static long UNSPECIFIED_TIME = LDMServesClient.UNSPECIFIED_TIME;

  /** External api for finding a property group by class at no specific time **/
  public final PropertyGroup searchForPropertyGroup(Class pgc) {
    PropertyGroup pg = resolvePG(pgc, UNSPECIFIED_TIME);
    return (pg instanceof Null_PG)?null:pg;
  }
      
  /** Convenient equivalent to searchForPropertyGroup(pg.getClass()) **/
  public final PropertyGroup searchForPropertyGroup(PropertyGroup pg) {
    return searchForPropertyGroup(pg.getClass());
  }

  /** External api for finding a property group by class at a specific time **/
  public final PropertyGroup searchForPropertyGroup(Class pgc, long t) {
    PropertyGroup pg = resolvePG(pgc, t);
    return (pg instanceof Null_PG)?null:pg;
  }

  /** Convenient equivalent to searchForPropertyGroup(pg.getClass(), time) **/
  public final PropertyGroup searchForPropertyGroup(PropertyGroup pg, long time) {
    return searchForPropertyGroup(pg.getClass(), time);
  }

  /** get and possibly cache a PG value.
   * The information can come from a number of places:
   *   a local slot 
   *   a late binding
   *   the prototype (recurse to resolve on the prototype)
   *   a default value
   *
   * Will return Null_PG instances if present.
   * implemented in Asset
   **/
  public abstract PropertyGroup resolvePG(Class pgc, long t);

  public final PropertyGroup resolvePG(Class pgc) {
    return resolvePG(pgc, UNSPECIFIED_TIME);
  }

  /** request late binding from the LDM for this asset/PGClass.
   * Late binders should set the asset's PG as appropriate in 
   * addition to returning the PG.
   *
   * Implemented in Asset
   *
   * @return null or a PropertyGroup instance.
   */
  protected abstract PropertyGroup lateBindPG(Class pgc, long t);

  public final PropertyGroup lateBindPG(Class pgc) {
    return lateBindPG(pgc, UNSPECIFIED_TIME);
  }

  /** generate and set a default PG instance (usually empty) for 
   * an asset.  Generally will just do a new.  Concrete.
   * Asset implementations will override this.
   **/
  protected PropertyGroup generateDefaultPG(Class pgc) {
    // if we wanted the PGs to *always* be there, we'd do something like:
    /*
    try {
      PropertyGroup pg = (PropertyGroup) pgc.newInstance();
      setLocalPG(pgc, pg);
      return pg;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
    */
    // but, the default case wants to just return null
    return null;
  }

  /** return the value of the specified PG if it is 
   * already present in a slot.
   **/
  protected synchronized PropertyGroup getLocalPG(Class pgc, long t) {
    if (otherProperties == null) { 
      return null;    
    }

    if (TimePhasedPropertyGroup.class.isAssignableFrom(pgc)) {
      int index = findLocalPGScheduleIndex(pgc);
      if (index >=0) {
        PropertyGroupSchedule pgs = (PropertyGroupSchedule) force().get(index);
        if (t == UNSPECIFIED_TIME) {
          return pgs.getDefault();
        } else {
          return (PropertyGroup) pgs.intersects(t);
        }
      } else {
        return null;
      }
    } else {
      int index = findLocalPGIndex(pgc);

      if (index >= 0) {
        return (PropertyGroup) force().get(index);
      } else {
        return null;
      }
    }
  }

  /** Set the apropriate slot in the asset to the specified pg.
   * Scheduled PGs have the time range in them, so the time (range)
   * should not be specified in the arglist.
   **/
  protected synchronized void setLocalPG(Class pgc, PropertyGroup prop) {
    if (prop instanceof TimePhasedPropertyGroup) {
      int index = findLocalPGScheduleIndex(pgc);
      TimePhasedPropertyGroup timePhasedProp = (TimePhasedPropertyGroup) prop;

      PropertyGroupSchedule schedule;
      if (index >= 0) {
        schedule = (PropertyGroupSchedule) force().get(index);
        schedule.removeAll(schedule.intersectingSet(timePhasedProp));
      } else {
        hasOtherTimePhasedProperties = true;
        schedule = new PropertyGroupSchedule();
        force().add(schedule);
      }

      schedule.add(prop);
    } else {
      addOrReplaceLocalPG(prop);
    }
  }

  protected synchronized void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {
    if (hasOtherTimePhasedProperties) {
      int index = findLocalPGScheduleIndex(pgSchedule.getPGClass());
      if (index >= 0) {
        force().remove(index);
      }
    } else {
      hasOtherTimePhasedProperties = true;
    }

    force().add(pgSchedule);
  }

  protected synchronized PropertyGroup removeLocalPG(Class c) {
    // Better be a property group
    // Need to verify because otherProperties contains both PGs and 
    // PGSchedules. Don't want to allow caller to remove an unspecified 
    // PGSchedule 
    if (!PropertyGroup.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException();
    }

    PropertyGroup removed = null;

    // Use removeOtherPropertyGroupSchedule to remove entire schedules.
    if (TimePhasedPropertyGroup.class.isAssignableFrom(c)) {

      int index = findLocalPGScheduleIndex(c);
      if (index >=0) {
        PropertyGroupSchedule pgs = (PropertyGroupSchedule) force().get(index);

        removed = pgs.getDefault();

        if ((removed == null) &&
            (pgs.size() > 0)) {
          removed = (PropertyGroup) pgs.get(0);
        }

        force().remove(index);
      }
    } else {
      int index = findLocalPGIndex(c);
      if (index >= 0) {
        removed = (PropertyGroup) force().get(index);
        force().remove(index);
      }
    } 
    return removed;
  }

  protected synchronized PropertyGroup removeLocalPG(PropertyGroup pg) {
    // Better be a property group
    // Need to verify because otherProperties contains both PGs and 
    // PGSchedules. Don't want to allow caller to remove an unspecified 
    // PGSchedule 
    if (!PropertyGroup.class.isAssignableFrom(pg.getPrimaryClass())) {
      throw new IllegalArgumentException();
    }

    PropertyGroup removed = null;
    Class pgc = pg.getPrimaryClass();

    // Use removeOtherPropertyGroupSchedule to remove entire schedules.
    if (TimePhasedPropertyGroup.class.isAssignableFrom(pgc)) {

      int index = findLocalPGScheduleIndex(pgc);
      if (index >=0) {
        PropertyGroupSchedule pgs = (PropertyGroupSchedule) force().get(index);

        if (pgs.getDefault() == pg) {
          pgs.clearDefault();
          removed = pg;
        } 
        
        if (pgs.remove(pg)) {
          removed = pg;
        }
      }
    } else {
      int index = findLocalPGIndex(pg.getPrimaryClass());
      if (index >= 0) {
        removed = (PropertyGroup) force().get(index);
        force().remove(index);
      }
    } 
    return removed;
  }

  protected synchronized PropertyGroupSchedule removeLocalPGSchedule(Class c) {
    int index = findLocalPGScheduleIndex(c);

    if (index >=0) {
      return (PropertyGroupSchedule) force().remove(index);
    } else {
      return null;
    }
  }

  /** add a PG, making sure to drop any previous PG of identical class which had
   * already been there.
   **/
  private final synchronized void addOrReplaceLocalPG(PropertyGroup prop) {
    // Look through the list for a PG of a matching class.  The hard part
    // of this is that either the prop or any of the elements of the list
    // may be natural (FooPGImpl), locked, Null, etc.  So: our solution is
    // to compare the "PrimaryClass" of each.
    int index = findLocalPGIndex(prop.getPrimaryClass());
    ArrayList ps = force();

    if (index >= 0) {
      ps.set(index, prop);
    } else {
      ps.add(prop);
    }
  }

  /** find index of specified PG in the set of additional properties.
   **/
  private final synchronized int findLocalPGIndex(Class propertyGroupClass) {
    // Look through the list for a PG of a matching class.  The hard part
    // of this is that either the prop or any of the elements of the list
    // may be natural (FooPGImpl), locked, Null, etc.  So: our solution is
    // to compare the "PrimaryClass" of each.
    ArrayList ps = force();
    int l = ps.size();

    for (int i = 0; i<l; i++) {
      Object o = ps.get(i);
      Class ok = null;

      if (o instanceof PropertyGroupSchedule) {
        // Don't bother with PropertyGroupSchedules
        continue;
      } else if (o instanceof PropertyGroup) {
        ok = ((PropertyGroup) o).getPrimaryClass();
      } else {
        throw new RuntimeException("Unable to handle object of Class: " + o.getClass() +
                                   " in otherProperties list.");
      }
      if (propertyGroupClass.equals(ok)) {
        return i;
      }
    }
    return -1;
  }

  /** find index of specified PropertyGroupSchedule in the set of additional
   * properties.
   **/
  private final synchronized int findLocalPGScheduleIndex(Class propertyGroupClass) {
    // Look through the list for a PG of a matching class.  The hard part
    // of this is that either the prop or any of the elements of the list
    // may be natural (FooPGImpl), locked, Null, etc.  So: our solution is
    // to compare the "PrimaryClass" of each.
    ArrayList ps = force();
    int l = ps.size();

    for (int i = 0; i<l; i++) {
      Object o = ps.get(i);
      Class ok = null;

      if (o instanceof PropertyGroup) {
        // Don't bother with PropertyGroups
        continue;
      } else if (o instanceof PropertyGroup) {
        ok = ((PropertyGroupSchedule) o).getPGClass();
      } else {
        throw new RuntimeException("Unable to handle object of Class: " + o.getClass() +
                                   " in otherProperties list.");
      }
      if (propertyGroupClass.equals(ok)) {
        return i;
      }
    }
    return -1;
  }
}





