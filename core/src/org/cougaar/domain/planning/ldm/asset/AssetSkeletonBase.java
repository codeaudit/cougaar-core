/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
import org.cougaar.util.*;
import java.beans.SimpleBeanInfo;

public abstract class AssetSkeletonBase
  extends SimpleBeanInfo
  implements Serializable, Cloneable 
{

  /** additional properties searched by default get*PG methods **/
  private ArrayList otherProperties = null;
  private boolean hasOtherTimePhasedProperties = false;

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

  /** @return the set of additional properties - not synchronized!**/
  public synchronized Enumeration getOtherProperties() {
    if (otherProperties == null || otherProperties.size()==0)
      return Empty.enumeration;
    else
      return new Enumerator(otherProperties);
  }

  /** replace the existing set of other properties **/
  protected synchronized void setOtherProperties( Collection newProps) {
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

  /** Add an OtherPropertyGroup to the set of additional properties **/
  public void addOtherPropertyGroup(PropertyGroup prop) {
    setLocalPG(prop.getPrimaryClass(), prop);
  }

  /** Add a PropertyGroupSchedule to the set of additional properties **/
  public synchronized void addOtherPropertyGroupSchedule(PropertyGroupSchedule prop) {
    PropertyGroupSchedule schedule = 
      searchForPropertyGroupSchedule(prop.getClass());

    if (schedule != null) {
      throw new IllegalArgumentException();
    } else {
      hasOtherTimePhasedProperties = true;

      force().add(prop);
    }
  }

  /** Add an OtherPropertyGroup to the set of additional 
   * properties, replacing any existing properties of the 
   * same type.
   **/
  public synchronized void replaceOtherPropertyGroup(PropertyGroup prop) {
    if (TimePhasedPropertyGroup.class.isAssignableFrom(prop.getClass())) {
      removeOtherPropertyGroup(prop);
      
      addOtherPropertyGroup(prop);
    } else {
      removeOtherPropertyGroup(prop.getClass());

      force().add(prop);
    }
  }

  /** Removes the instance matching the class passed in as an argument 
   * Note: this implementation assumes that OtherPropertyGroup holds one 
   * and only one instance of a given class.
   * Also, this method <i>must</i> be invoked if you 
   * are replacing a property.  Otherwise, you will 
   * be adding a duplicate property which will be an error. 
   * @param c Class to match.
   * @return PropertyGroup Return the property instance that was removed; 
   * otherwise, return null.
   **/ 
  public synchronized PropertyGroup removeOtherPropertyGroup(Class c) {
    if (otherProperties == null) {
      return null;
    }

    // Better be a property group
    // Need to verify because otherProperties contains both PGs and PGSchedules.
    // Don't want to allow caller to remove an unspecified PGSchedule 
    if (!PropertyGroup.class.isAssignableFrom(c)) {
      throw new IllegalArgumentException();
    }

    boolean isTimePhased = TimePhasedPropertyGroup.class.isAssignableFrom(c);
    Object last = null;
    Iterator i = otherProperties.iterator();
    while (i.hasNext()) {
      Object p = i.next();
      
      if (!isTimePhased) {
        if (c.isInstance(p)) {
          last = p;
          i.remove();
        }
      } else {
        if ((p instanceof PropertyGroupSchedule) &&
            (((PropertyGroupSchedule)p).getPGClass().equals(c))) {
          last = ((PropertyGroupSchedule)p).getDefault();
          
          //Didn't have a default so we've got to iterate
          if (last == null) {
            for (Iterator j = ((PropertyGroupSchedule)p).iterator();
                 j.hasNext();) {
              last = j.next();
              break;
            }
          }

          i.remove();
        }
      }
          
    }
    return (PropertyGroup)last;
  }

  public synchronized PropertyGroup removeOtherPropertyGroup(PropertyGroup pg) {
    if (otherProperties == null) {
      return null;
    }

    boolean isTimePhased = 
      TimePhasedPropertyGroup.class.isAssignableFrom(pg.getClass());

    Object last = null;
    Iterator i = otherProperties.iterator();
    while (i.hasNext()) {
      Object p = i.next();

      if (!isTimePhased) {
        if (pg.equals(p)){
          last = p;
          i.remove();
        }
      } else if ((p instanceof PropertyGroupSchedule) &&
                 (((PropertyGroupSchedule)p).getPGClass().equals(pg.getClass()))) {
        TimePhasedPropertyGroup tpppg = (TimePhasedPropertyGroup)pg;
        
        // BOZO - Should we allow them to remove the default?
        if (((PropertyGroupSchedule)p).remove(p)) {
          last = p;
        } else {
          last = null;
        }
      }
    }

    return (PropertyGroup) last;
  }

  /** return the time-phased schedule associated with the specified PropertyGroup
   * class. 
   **/
  public synchronized PropertyGroupSchedule searchForPropertyGroupSchedule(Class c) {
    if (otherProperties == null) { 
      return null;    
    }

    // Use time phased method
    if (!TimePhasedPropertyGroup.class.isAssignableFrom(c)) {
      return null;
    }

    Iterator i = otherProperties.iterator();
    while (i.hasNext()) {
      Object p = i.next();

      if (p instanceof PropertyGroupSchedule) {
        if (((PropertyGroupSchedule)p).getPGClass().equals(c)) {
          // BOZO - Should we clone?
          return (PropertyGroupSchedule)p;
        }
      }
    }
    return null;
  }


  /** Convenient equivalent to searchForPropertyGroupSchedule(pg.getClass()) **/
  public final PropertyGroupSchedule searchForPropertyGroupSchedule(PropertyGroup pg) {
    return searchForPropertyGroupSchedule(pg.getClass());
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

    Iterator i = otherProperties.iterator();
    while (i.hasNext()) {
      Object p = i.next();
      if ((p instanceof PropertyGroupSchedule)) {
        PropertyGroupSchedule pgs = (PropertyGroupSchedule) p;
        if (pgc.isAssignableFrom(pgs.getPGClass())) {
          if (t == UNSPECIFIED_TIME) {
            return pgs.getDefault();
          } else {
            return (PropertyGroup) pgs.intersects(t);
          }
        }
      } else if (pgc.isAssignableFrom(p.getClass())) {
        return (PropertyGroup)p;
      }
    }
    return null;
  }

  /** Set the apropriate slot in the asset to the specified pg.
   * Scheduled PGs have the time range in them, so the time (range)
   * should not be specified in the arglist.
   **/
  protected void setLocalPG(Class pgc, PropertyGroup prop) {
    if (prop instanceof TimePhasedPropertyGroup) {
      PropertyGroupSchedule schedule = searchForPropertyGroupSchedule(pgc);
      if (schedule != null) {
        schedule.add(prop);
      } else {
        hasOtherTimePhasedProperties = true;
        // ??? Should this first value be a default
        schedule = new PropertyGroupSchedule((TimePhasedPropertyGroup)prop);
        force().add(schedule);
      }
    } else {
      addOrReplaceLocalPG(pgc, prop);
    }
  }

  /** add a PG, making sure to drop any previous PG of identical class which had
   * already been there.
   **/
  private final synchronized void addOrReplaceLocalPG(Class pgc, PropertyGroup prop) {
    // Look through the list for a PG of a matching class.  The hard part
    // of this is that either the prop or any of the elements of the list
    // may be natural (FooPGImpl), locked, Null, etc.  So: our solution is
    // to compare the "PrimaryClass" of each.
    ArrayList ps = force();

    Class key = prop.getPrimaryClass();
    int l = ps.size();
    for (int i = 0; i<l; i++) {
      PropertyGroup op = (PropertyGroup) ps.get(i);
      Class ok = op.getPrimaryClass();
      if (key.equals(ok)) {
        ps.set(i, prop);
        return;
      }
    }
    ps.add(prop);
  }
}
