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

import org.cougaar.domain.planning.ldm.plan.ItineraryElement;
import org.cougaar.domain.planning.ldm.plan.NewItineraryElement;
import org.cougaar.domain.planning.ldm.plan.Verb;
import org.cougaar.domain.planning.ldm.plan.Location;
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
