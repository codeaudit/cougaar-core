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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.util.UID;

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




