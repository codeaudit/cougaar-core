/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
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
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.core.cluster.Distributor;
import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterServesPlugIn;
import org.cougaar.core.cluster.UIDServer;
import org.cougaar.core.plugin.PlugInDelegate;

/** A plugin's view of its parent component (Container).
 *
 **/
public interface PluginBindingSite 
  extends BindingSite
{
  ClusterIdentifier getAgentIdentifier();

  ConfigFinder getConfigFinder();

  UIDServer getUIDServer();
}
