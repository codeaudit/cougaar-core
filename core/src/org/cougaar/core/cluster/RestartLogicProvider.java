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

/** API for LogPlan LogicProviders which handle cluster restarts
 **/

public interface RestartLogicProvider extends LogicProvider {

  /**
   * Called by LogPlan whenever another cluster restarts. Allows an
   * opportunity for the logic provider to reconcile the state of the
   * other cluster with this one.
   * @param cid the ClusterIdentifier of the other cluster
   **/
  void restart(ClusterIdentifier cid);
}
