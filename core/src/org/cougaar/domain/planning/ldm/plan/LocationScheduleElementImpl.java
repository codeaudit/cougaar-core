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

import org.cougaar.domain.planning.ldm.plan.NewLocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Location;
import java.util.Date;


/**
 * A LocationScheduleElement is an encapsulation of temporal relationships
 * and a location over that time interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationScheduleElementImpl.java,v 1.2 2001-04-05 19:27:15 mthome Exp $
 **/

public class LocationScheduleElementImpl 
  extends ScheduleElementImpl
  implements LocationScheduleElement, NewLocationScheduleElement 
{
	
  private Location location;
	
  /** no-arg constructor */
  public LocationScheduleElementImpl () {
    super();
  }
	
  /** constructor for factory use that takes the start and end dates and a location*/
  public LocationScheduleElementImpl(Date start, Date end, Location l) {
    super(start, end);
    location = l;
  }

  /** constructor for factory use that takes the start and end times and a location*/
  public LocationScheduleElementImpl(long start, long end, Location l) {
    super(start, end);
    location = l;
  }
	
  /** @return Location location related to this schedule */
  public Location getLocation() {
    return location;
  }	
		
  // NewLocationScheduleElement interface implementations
	
  /** @param aLocation set the location related to this schedule */
  public void setLocation(Location aLocation) {
    location = aLocation;
  }
} 
