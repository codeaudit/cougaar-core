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
 * Verification Interface Verification is a response to an asset
 * transfer that was sent to a cluster.  The Verification will include
 * the asset the asset that was transferred. Thiis only actually used
 * to verify if an apparent transfer transfer is still valid after a
 * cluster restarts.
 **/

public interface AssetVerification extends Directive {
  Asset getAsset();
  Asset getAssignee();
  Schedule getSchedule();
}



