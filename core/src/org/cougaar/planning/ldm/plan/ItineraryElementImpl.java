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

import org.cougaar.planning.ldm.plan.ItineraryElement;
import org.cougaar.planning.ldm.plan.NewItineraryElement;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.planning.ldm.plan.Location;
import java.util.Date;

/**
 * Implement an element of an Itinerary.
 **/

public class ItineraryElementImpl extends LocationRangeScheduleElementImpl
  implements ItineraryElement, NewItineraryElement
{
  private Verb role = null;
	
  /** no-arg constructor */
  public ItineraryElementImpl() {
    super();
  }
	
  /** Complete constructor
   **/
  public ItineraryElementImpl(Date start, Date end, Location sl, Location el, Verb role) {
    super(start, end, sl, el);
    this.role = role;
  }
	
  public Verb getRole() {
    return role;
  }

  public void setRole(Verb role) {
    this.role = role;
  }
	
} 
