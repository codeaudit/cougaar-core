/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

/** A Claimable object is something that may be "claimed" by a single
 * actor, generally an instance of a plugin.  For instance, a Claimable Task
 * would likely be "claimed" by a plugin when that plugin creates the
 * task or the task is entered into the logplan.
 *
 * Claiming of objects is done by the infrastruture *only* - plugins should
 * *never* call claim().
 * 
 **/

public interface Claimable 
{
  /** @return true IFF this object been claimed. **/
  boolean isClaimed();

  /** @return the current claim holder, or null if there is none. **/
  Object getClaim();

  /** Stake a Claim on the object.
   * @exception IllegalArgumentException If there is already a Claim on the object which is not == the putativeClaimHolder.
   **/
  void setClaim(Object putativeClaimHolder);

  /** Try to stake a Claim on the object.
   * @return true IFF success.
   **/
  boolean tryClaim(Object putativeClaimHolder);

  /** Release a Claim on the object.
   * @exception IllegalArgumentExcpeiton If the object is not currently 
   * claimed, or is claimed by someone else.
   **/
  void resetClaim(Object oldClaimHolder);
}
  
