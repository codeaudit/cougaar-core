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

import org.cougaar.domain.planning.ldm.asset.Asset;

/**
 * AssetAssignment Setter Interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewAssetAssignment.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
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
