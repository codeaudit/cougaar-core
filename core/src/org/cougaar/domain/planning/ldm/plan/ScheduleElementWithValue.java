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

import java.util.Date;

/**
 * ScheduleElementWithValue includes an untyped double value at each
 * element.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: ScheduleElementWithValue.java,v 1.2 2001-04-05 19:27:20 mthome Exp $
 **/

public interface ScheduleElementWithValue extends ScheduleElement
{
  /** return the untyped value at this schedule point **/
  double getValue();

  /** return a new scheduleElement of the same type as this 
   * with the specified time span and value.
   **/
  ScheduleElementWithValue newElement(long start, long end, double value);
}
