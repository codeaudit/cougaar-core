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

/** Allocation Interface
 * An Allocation is a type of PlanElement
 * which represents the Asset that will complete
 * the Task.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Allocation.java,v 1.3 2001-08-22 20:14:08 mthome Exp $
 **/

public interface Allocation extends PlanElement {
	
  /** Returns an Asset that has certain capabilities.
   * This Asset is assigned to complete the Task that is
   * matched with the Allocation in the PlanElement.
   *
   * @return Asset - a physical entity or cluster that is assigned to perform the Task.
   **/
		
  org.cougaar.domain.planning.ldm.asset.Asset getAsset();
   
  /** Checks to see if there is a potential conflict with another allocation
   * or asset transfer involving the same asset.
   * Will return true if there is a potential conflict.
   * Will return false if there is NOT a potential conflict.
   * @return boolean
   */
  boolean isPotentialConflict();
  
  /** Checks to see if there is a potential conflict with the asset's
   * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
   * Will return true if there is a potential conflict.
   * @return boolean
   */
  boolean isAssetAvailabilityConflict();
  
  /** Check to see if this allocation is Stale and needs to be revisited.
   * Will return true if it is stale (needs to be revisted)
   * @return boolean
   */
  boolean isStale();
  
  /** Set the stale flag.  Usualy used by Trigger actions.
   * @param stalestate
   */
  void setStale(boolean stalestate);
  
  /** Return the Role that the Asset is performing while executing
   * this PlanElement (Task).  
   *
   * @return Role
   **/
  Role getRole();
}
