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
 * A LocationRangeScheduleElement is a subclass of ScheduleElement which provides
 * a two slots for a start and end location 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationRangeScheduleElement.java,v 1.2 2001-04-05 19:27:15 mthome Exp $
 **/

public interface LocationRangeScheduleElement extends ScheduleElement {
	
  /** @return Location start location related to this schedule */
  Location getStartLocation();
	
  /** @return Location end location related to this schedule */
  Location getEndLocation();
}
