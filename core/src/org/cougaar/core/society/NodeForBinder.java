/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.society;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.MessageTransportException;
import org.cougaar.core.mts.MessageTransportService;

/** This is the interface presented to an AgentManagerBinder from the Node.
 **/
public interface NodeForBinder
  extends ContainerAPI
{
  String getIdentifier();
  void registerCluster(ClusterServesClusterManagement cluster);
  String getName();
}
