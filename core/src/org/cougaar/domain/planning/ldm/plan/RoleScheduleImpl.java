/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;
import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** RoleSchedule Implementation
 * A RoleSchedule is a representation of an asset's scheduled
 * commitments. These commitments(plan elements) are stored
 * in a collection.  RoleSchedules do not travel with an
 * asset accross cluster boundaries, therefore, the roleschedule 
 * is only valid while that asset is assigned to the current cluster.
 **/

public class RoleScheduleImpl 
  extends ScheduleImpl
  implements RoleSchedule, NewRoleSchedule
{
  private transient Schedule availableschedule;
  private Asset asset;
	
  /** Constructor
   * @param Asset the asset this roleschedule is attached to
   **/
  public RoleScheduleImpl(Asset theasset) {
    super();
    setScheduleType(ScheduleType.ROLE);
    setScheduleElementType(ScheduleElementType.PLAN_ELEMENT);
    asset = theasset;
  }
	
  /** @return the Asset of this roleschedule.
   **/
  public Asset getAsset() {
    return asset;
  }
	
  /** SHOULD *ONLY* BE CALLED BY THE ASSET CREATOR or THE ASSETTRANSFER LP!
   * set the availableschedule
   * @param avschedule - the schedule that the asset is assigned 
   * or available to this cluster
   **/
  public void setAvailableSchedule(Schedule avschedule) {
    availableschedule = avschedule;
  }

  //return the available schedule for this asset
  public Schedule getAvailableSchedule() {
    return availableschedule;
  }

  /**
   *  ALPINE INTERNAL METHOD - SHOULD NEVER BE CALLED BY A PLUGIN
   *  add a single planelement to the roleschedule container
   *  @param aPlanElement PlanElement to add
   *  @deprecated Use add(Object aPlanElement) instead.
   **/
  public synchronized void addToRoleSchedule(PlanElement aPlanElement) {
    add(aPlanElement);
  }
	
  /**
   *  ALPINE INTERNAL METHOD - SHOULD NEVER BE CALLED BY A PLUGIN
   *  remove a single planelement from the roleschedule container
   *  @param aPlanElement PlanElement to remove
   *  @deprecated Use remove(Object aPlanElement) instead.
   **/
  public synchronized void removeFromRoleSchedule(PlanElement aPlanElement) {
    remove(aPlanElement);
  }

  public Collection getEncapsulatedRoleSchedule(Date start, Date end) {
    return getEncapsulatedRoleSchedule(start.getTime(), end.getTime());
  }

  public synchronized Collection getEncapsulatedRoleSchedule(long start, long end) {
    return getEncapsulatedScheduleElements(start, end);
  }

  public synchronized Collection getEqualAspectValues(final int aspect, final double value) {
    return filter(new UnaryPredicate() {
        public boolean execute(Object obj) {
          AllocationResult ar = ((PlanElement)obj).getEstimatedResult();
          if (ar != null) {
            return (value == ar.getValue(aspect));
          }
          return false;
        }
      });
  }
  
  public synchronized Collection getMatchingRoleElements(final Role aRole) {
    return filter(new UnaryPredicate() {
        public boolean execute (Object obj) {
          if (obj instanceof Allocation) {
            Role disrole = ((Allocation)obj).getRole();
            if (disrole.equals(aRole)){
              return true;
            }
          } else if (obj instanceof AssetTransfer) {
            Role disrole = ((AssetTransfer)obj).getRole();
            if (disrole.equals(aRole)) {
              return true;
            }
          }
          return false;
        }
      });
  }
  
  public Collection getOverlappingRoleSchedule(Date start, Date end) {
    return getOverlappingRoleSchedule(start.getTime(), end.getTime());
  }
  public synchronized Collection getOverlappingRoleSchedule(long start, long end) {
    return getOverlappingScheduleElements(start,end);
  }
  
  /** get an enumeration over a copy of all of the schedule elements of this 
   * schedule.
   * Note that this is a copy, changes to the underlying schedule will not be 
   * reflected in the Enumeration.
   * @return Enumeration{ScheduleElement}
   */
  public Enumeration getRoleScheduleElements() {
    return getAllScheduleElements();
  }

  /** Convenience utility that adds the requested aspectvalues of the estimated
    * allocationresult of each PlanElement (RoleSchedule Element) in the given
    * orderedset.
    * If the requested aspecttype is not defined for any of the elements, nothing
    * will be added to the sum for that particular element.
    * This utility should be used to add aspecttypes like quantity, cost, etc.
    * @return double The sum of the aspectvalues
    * @param elementsToAdd  A set of roleschedule elements (planelements) to add
    * @see org.cougaar.domain.planning.ldm.plan.AspectType
    **/
  public double addAspectValues(Collection elementsToAdd, 
                                       int aspecttype) {
    double acc = 0.0;

    synchronized (elementsToAdd) {
      if (elementsToAdd instanceof List) {
        int listSize = elementsToAdd.size();
        
        for (int index = 0; index < listSize; index++) {
          PlanElement anElement = 
            (PlanElement)((List) elementsToAdd).get(index);
          AllocationResult aResult = anElement.getEstimatedResult();
          if (aResult != null && aResult.isDefined(aspecttype)) {
            acc += aResult.getValue(aspecttype);
          }
        }
      } else {
        for (Iterator i = elementsToAdd.iterator(); i.hasNext(); ) {
          PlanElement anElement = (PlanElement)i.next();
          AllocationResult aResult = anElement.getEstimatedResult();
          if (aResult != null && aResult.isDefined(aspecttype)) {
            acc += aResult.getValue(aspecttype);
          }
        }
      }
    } // end synchronization on elementsToAdd

    return acc;
  }
      
  // for BeanInfo
  public synchronized String[] getRoleScheduleIDs() {
    int l = size();
    String[] IDs = new String[l];
    for (int i = 0; i < l; i++) {
      IDs[i] = ((PlanElement)get(i)).getUID().toString();
    }
    return IDs;
  }

  public String getRoleScheduleID(int i) {
    return ((PlanElement)get(i)).getUID().toString();
  }

}




