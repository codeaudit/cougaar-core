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
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.asset.Asset;

/** AssetTransfer Interface
 * An AssetTransfer is a type of PlanElement
 * which represents an Asset being assigned to another Cluster for use.
 * An AssetAssignment Directive is closely related
 *
 **/

public interface AssetTransfer extends PlanElement {
	
  /** Returns an Asset that has certain capabilities.
   * This Asset is being assigned to a cluster for use.
   *
   * @return org.cougaar.domain.planning.ldm.asset.Asset - a physical entity or cluster that is assigned to a Cluster.
   **/
		
  Asset getAsset();
 	
  /** Returns the Asset to which the asset is being assigned.
   * @return Asset representing the destination asset
   */
 	
  Asset getAssignee();
 
  /** Returns the Cluster from which the asset was assigned.
   * @return ClusterIdentifier representing the source of the asset
   */
 	
  ClusterIdentifier getAssignor();
 
  /** Returns the Schedule for the "ownership" of the asset being transfered.
   *  @return Schedule
   */
  Schedule getSchedule();
  
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

 
  /**
   * request that the destination organization be re-contacted due 
   * to changes in the transferred asset (e.g. Organization predictor
   * has been modified.  The AssetTransfer object also should be 
   * publishChange()ed.
   **/
  void indicateAssetChange();

  /** infrastructure hook for resetting AssetChange flag **/
  void resetAssetChangeIndicated();

  /** is there an unprocessed asset change pending?
   **/
  boolean isAssetChangeIndicated();
  
  /** Return the Role this Asset is performing while transferred.
   *  @return Role
   **/
  Role getRole();
  
      
}
