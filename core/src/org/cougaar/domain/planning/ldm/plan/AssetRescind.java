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

/** AssetRescind Interface
 * AssetRescind allows an asset to be rescinded from the Plan. 
 **/

public interface AssetRescind extends Directive {

  /**
   * Returns the asset to be rescinded
   * @return Asset
   **/
  Asset getAsset();

  /**
   * Returns the asset from which the transfer will be rescinded
   * @return Asset
   **/
  Asset getRescindee();

  /**
   * Returns the schedule for which the transfer will be rescinded
   * @return Asset
   **/
  Schedule getSchedule();


}
