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

/**
 * a Schedule element representing a Role-marked span in time and space.
 **/

public interface ItineraryElement extends LocationRangeScheduleElement {
  /** @return the role being exercised in the itinerary. **/
  Verb getRole();

}
