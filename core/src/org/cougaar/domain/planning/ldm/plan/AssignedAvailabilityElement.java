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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.TimeSpan;

/**
 * AssignedAvailabilityElement represents the availability to a specific asset
 * for the specified time interval.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AssignedAvailabilityElement.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
 **/


public interface AssignedAvailabilityElement 
  extends ScheduleElement
{
	
  /** Asset to which the asset is being assigned
   * @return Asset
   **/
  Asset getAssignee();
} 
