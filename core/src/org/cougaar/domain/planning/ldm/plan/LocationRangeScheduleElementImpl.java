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

import org.cougaar.domain.planning.ldm.plan.NewLocationRangeScheduleElement;
import org.cougaar.domain.planning.ldm.plan.LocationRangeScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Location;
import java.util.Date;


/**
 * A LocationRangeScheduleElement is an encapsulation of temporal relationships
 * and locations over that interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationRangeScheduleElementImpl.java,v 1.3 2001-08-22 20:14:11 mthome Exp $
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
