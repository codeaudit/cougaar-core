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
 * NewLocationRangeScheduleElement provides setters to build a complete object.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewLocationRangeScheduleElement.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 **/

public interface NewLocationRangeScheduleElement extends LocationRangeScheduleElement, NewScheduleElement {

  /** @param aStartLocation set the start location related to this schedule */
  void setStartLocation(Location aStartLocation);
        
  /** @param anEndLocation set the end location related to this schedule */
  void setEndLocation(Location anEndLocation);
}
