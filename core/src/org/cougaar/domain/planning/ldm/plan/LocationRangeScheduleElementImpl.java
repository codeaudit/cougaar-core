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

import org.cougaar.domain.planning.ldm.plan.NewLocationRangeScheduleElement;
import org.cougaar.domain.planning.ldm.plan.LocationRangeScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Location;
import java.util.Date;


/**
 * A LocationRangeScheduleElement is an encapsulation of temporal relationships
 * and locations over that interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationRangeScheduleElementImpl.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public class LocationRangeScheduleElementImpl extends ScheduleElementImpl
  implements LocationRangeScheduleElement, NewLocationRangeScheduleElement {
        
  private Location sloc, eloc;
        
  /** no-arg constructor */
  public LocationRangeScheduleElementImpl () {
    super();
  }
        
  /** constructor for factory use that takes the start and end dates and a
   * start and end locations*/
  public LocationRangeScheduleElementImpl(Date start, Date end, Location sl, Location el) {
    super(start, end);
    sloc = sl;
    eloc = el;
  }
        
  /** @return Location start location related to this schedule */
  public Location getStartLocation() {
    return sloc;
  }
        
  /** @return Location end location related to this schedule */
  public Location getEndLocation() {
    return eloc;
  }
                
        
  // NewLocationRangeScheduleElement interface implementations
        
  /** @param aStartLocation set the start location related to this schedule */
  public void setStartLocation(Location aStartLocation) {
    sloc = aStartLocation;
  }
        
  /** @param anEndLocation set the end location related to this schedule */
  public void setEndLocation(Location anEndLocation) {
    eloc = anEndLocation;
  }

} 
