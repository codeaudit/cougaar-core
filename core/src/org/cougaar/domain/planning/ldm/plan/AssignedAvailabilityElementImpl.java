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

/**
 * AssignedAvailabilityElement represents the availability to a specific asset
 * over a time interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AssignedAvailabilityElementImpl.java,v 1.2 2001-01-04 19:14:21 ngivler Exp $
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
