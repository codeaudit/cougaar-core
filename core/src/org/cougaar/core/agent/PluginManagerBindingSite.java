/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.ClusterIdentifier;

/** A pluginmanager's view of its parent component (Container).
 *
 **/
public interface PluginManagerBindingSite 
  extends BindingSite
{
  ClusterIdentifier getAgentIdentifier();
  ConfigFinder getConfigFinder();
}


