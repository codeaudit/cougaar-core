/*
 * <Copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.util.Collection;
import java.util.Iterator;

/** 
 * A AssignedRelationshipSchedule is a Schedule of 
 * AssignedRelationshipElements. Should only be used by the logic providers in
 * handling add/modify/remove of AssetTransfers.
 **/

public class AssignedRelationshipScheduleImpl extends ScheduleImpl {

  public AssignedRelationshipScheduleImpl() {
    super();
    setScheduleType(ScheduleType.ASSIGNED_RELATIONSHIP);
    setScheduleElementType(ScheduleElementType.ASSIGNED_RELATIONSHIP);
  }

  public AssignedRelationshipScheduleImpl(Collection AssignedRelationships) {
    this();
    
    addAll(AssignedRelationships);
  }
  
  /* @returns Iterator over the schedule. Surfaced because 
   * AssignedRelationshipScheduleImpl should only be used by the logic
   * providers.
   */
  public synchronized Iterator iterator() {
    return protectedIterator();
  }

}




