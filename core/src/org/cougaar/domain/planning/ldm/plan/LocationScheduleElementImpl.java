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

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.NewLocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Location;
import java.util.Date;


/**
 * A LocationScheduleElement is an encapsulation of temporal relationships
 * and a location over that time interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationScheduleElementImpl.java,v 1.3 2001-08-22 20:14:11 mthome Exp $
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
