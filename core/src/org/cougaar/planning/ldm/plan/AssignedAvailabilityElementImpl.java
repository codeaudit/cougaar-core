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

import org.cougaar.planning.ldm.asset.Asset;

/**
 * AssignedAvailabilityElement represents the availability to a specific asset
 * over a time interval.
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/


public class AssignedAvailabilityElementImpl extends ScheduleElementImpl
  implements NewAssignedAvailabilityElement {

  private Asset myAssignee;

  /** constructor for factory use */
  public AssignedAvailabilityElementImpl() {
    super();
    setAssignee(null);
  }

  /** constructor for factory use that takes the start, end times & the
   *  assignee asset
  **/
  public AssignedAvailabilityElementImpl(Asset assignee, long start, long end) {
    super(start, end);
    setAssignee(assignee);
  }
        
  public Asset getAssignee() { 
    return myAssignee; 
  }

  public void setAssignee(Asset assignee) {
    myAssignee = assignee;
  }

  /** 
   * equals - performs field by field comparison
   *
   * @param object Object to compare
   * @return boolean if 'same' 
   */
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if (!(object instanceof AssignedAvailabilityElement)) {
      return false;
    }

    AssignedAvailabilityElement other = (AssignedAvailabilityElement)object;

    
    return (getAssignee().equals(other.getAssignee()) &&
            getStartTime() == other.getStartTime() &&
            getEndTime() == other.getEndTime());
  }
  
}
