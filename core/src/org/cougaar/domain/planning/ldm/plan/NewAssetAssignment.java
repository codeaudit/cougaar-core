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

import org.cougaar.domain.planning.ldm.asset.Asset;

/**
 * AssetAssignment Setter Interface
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/
	
public interface NewAssetAssignment extends AssetAssignment, NewDirective  {
		
  /** @param newasset - sets the asset being assigned 
   */
  void setAsset(Asset newasset);
		
  /** 
   * Sets the schedule or time frame that the asset will be assigned.
   * @param sched - The time frame that the Asset will be assigned
   **/
  void setSchedule(Schedule sched);

  /** @param newasset - sets the asset to receive the assigned asset
   */
  void setAssignee(Asset newasset);

  /**
   * @param newKind The kind code (NEW, UPDATE, REPEAT) of this
   * assignment.
   **/
  void setKind(byte newKind);
}
