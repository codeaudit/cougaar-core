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

package org.cougaar.core.blackboard;

import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

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
  
