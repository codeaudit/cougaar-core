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

/**
 * A LocationScheduleElement is a subclass of ScheduleElement which provides
 * a slot for a location 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: LocationScheduleElement.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface LocationScheduleElement extends ScheduleElement {
	
	/** @return Location location related to this schedule */
	Location getLocation();
	

}