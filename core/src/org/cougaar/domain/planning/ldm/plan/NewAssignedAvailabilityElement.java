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
 
 /** NewAssignedAvailabilityElement extends AssignedAvailabilityElement and
   * provides setter methods for building valid AssignedAvailabilityElement 
   * objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewAssignedAvailabilityElement.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
   **/
 	 
public interface NewAssignedAvailabilityElement extends AssignedAvailabilityElement, NewScheduleElement {
 	
  /** @param assignee Asset to which the transfered asset is assigned */
  void setAssignee(Asset assignee);
}


