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

/** NewRoleSchedule Interface
 *  Allows a setter for newly created assets to set their available schedule
 *
 **/

public interface NewRoleSchedule extends RoleSchedule {


  /** SHOULD *ONLY* BE CALLED BY THE ASSET CREATOR or THE ASSETTRANSFER LP!
    * set the availableschedule
    * The AvailableSchedule represents the time period that this asset
    * is assigned to a cluster for use.  It does not represent any usage
    * of this asset - that information is elsewhere in the RoleSchedule.
    * @param avschedule - the schedule that the asset is assigned 
    * or available to this cluster
    **/
  public void setAvailableSchedule(Schedule avschedule);
  
}
