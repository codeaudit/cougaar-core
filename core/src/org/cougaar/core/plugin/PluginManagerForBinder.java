/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.cluster.*;

/** This is the interface presented to a PluginBinder from the PluginManager.
 **/
public interface PluginManagerForBinder
  extends ContainerAPI
{
  ClusterIdentifier getAgentIdentifier();
  UIDServer getUIDServer();
  ConfigFinder getConfigFinder();
}
