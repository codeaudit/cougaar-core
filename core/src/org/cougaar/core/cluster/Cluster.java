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

/** Group together all the interfaces that a Cluster must implement
 * into a single class specification.
 * No client should ever cast to Cluster and no member or argument 
 * should be typed as Cluster, rather this class is intended to be
 * a precise specification of what a whole cluster must provide.
 * In practice, however, this functionality may be split into separate
 * modules and combined (or not) for actual cluster implementations.
 *
 * Actual Cluster implementation are likely to implement more standard
 * modules directly.  For example, org.cougaar.core.cluster.ClusterImpl
 * also implements LDMServesPlugIn, ClusterContext, and IMessageTransport.
 **/

public interface Cluster extends ClusterStateModel,
  ClusterServesClusterManagement,
  ClusterServesMessageTransport,
  ClusterServesLogicProvider,
  ClusterServesPlugIn
{
}

