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
  ClusterIdentifier getClusterIdentifier();

  // these should be part of LDMService
  LDMServesPlugIn getLDM();
  RootFactory getFactory();
  Factory getFactory(String s);
  UIDServer getUIDServer();

  // these should be part of ThreadingService/SchedulerService

  public final static int UNSPECIFIED_THREAD = -1;
  public final static int NO_THREAD = 0;
  public final static int SHARED_THREAD = 1;
  public final static int SINGLE_THREAD = 2;
  public final static int ONESHOT_THREAD = 3;

  /** Set the current choice of threading model.  Will have no effect if
   * the threading model has already been acted on.
   **/
  void setThreadingChoice(int m);

  /** @return the current choice of threading model.  **/
  int getThreadingChoice();

  ConfigFinder getConfigFinder();

}
