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

/** Allocation Interface
 * An Allocation is a type of PlanElement
 * which represents the Asset that will complete
 * the Task.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Allocation.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
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
