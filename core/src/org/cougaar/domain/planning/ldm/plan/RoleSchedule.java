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

import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

/** 
 * A RoleSchedule is a representation of an asset's scheduled
 * commitments. These commitments(plan elements) are stored
 * in a Collection.  RoleSchedules do not travel with an
 * asset accross cluster boundaries, therefore, the roleschedule 
 * is only valid while that asset is assigned to the current cluster.
 **/

public interface RoleSchedule 
  extends Collection
{
  /** @return Asset the Asset of this roleschedule.
   **/
  Asset getAsset();

  /** @return a sorted Collection containing planelements
   * whose estimatedschedule falls within the given date range.
   * @param start Start time of the desired range.
   * @param end End time of the desired range.
   **/
  Collection getEncapsulatedRoleSchedule(long start, long end);
  /** @deprecated use getEncapsulatedRoleSchedule(long, long) **/
  Collection getEncapsulatedRoleSchedule(Date start, Date end);
  
  /** @return a sorted Collection containing planelements with
   * a given AspectType and value.
   * @param int  The AspectType
   * @param value The double representing value of the given AspectType
   **/
  Collection getEqualAspectValues(int aspecttype, double value);
	
  /** @return a sorted collection containing planelements
   * whose estimatedschedule overlaps with the given date range
   * @param start Start time of overlapping range
   * @param end End time of overlapping range
   **/
  Collection getOverlappingRoleSchedule(long start, long end);

  /** @deprecated use getOverlappingRoleSchedule(long, long) **/
  Collection getOverlappingRoleSchedule(Date start, Date end);
	
  /** @return an Enumeration of PlanElements representing 
   * the entire roleschedule
   **/
  Enumeration getRoleScheduleElements();
  
  /** The AvailableSchedule represents the time period that this asset
   * is assigned to a cluster for use.  It does not represent any usage
   * of this asset - that information is elsewhere in the RoleSchedule.
   *
   * @return the schedule marking the availability time frame for the asset
   * in this cluster.
   **/
  Schedule getAvailableSchedule();
  
  /** @return a time ordered Collection containing planelements with a given Role.
   * @param aRole  The Role to find
   **/
  Collection getMatchingRoleElements(Role aRole);

  /** 
   * @return a time sorted Collection of planelements which
   * include this time.
   */
  Collection getScheduleElementsWithTime(long aDate);
  /** @deprecated use getScheduleElementsWithTime(long) **/
  Collection getScheduleElementsWithDate(Date aDate);
  
  /** Convenience utility that adds the requested aspectvalues of the estimated
   * allocationresult of each PlanElement (RoleSchedule Element) in the given
   * collection.
   * If the requested aspecttype is not defined for any of the elements, nothing
   * will be added to the sum for that particular element.
   * This utility should be used to add aspecttypes like quantity, cost, etc.
   * @return The sum of the aspectvalues
   * @param elementsToAdd  A set of roleschedule elements (planelements) to add
   * @see org.cougaar.domain.planning.ldm.plan.AspectType
   **/
  double addAspectValues(Collection elementsToAdd, int aspecttype);
}
