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
 * @version $Id: AssignedAvailabilityElementImpl.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
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

  
}
