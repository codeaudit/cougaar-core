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
import org.cougaar.core.cluster.*;

/** An immediate child component's view of it's Parent
 *
 **/
public interface AgentChildBindingSite 
  extends BindingSite
{
  ClusterIdentifier getAgentIdentifier();
  ConfigFinder getConfigFinder();
  /** Temporarly hack to allow getting at cluster services
   * from LPs (and blackboard).  This will be replaced with
   * some of the methods defined by ClusterServesLogicProvider 
   * which are actually used by Blackboard.
   **/
  ClusterServesLogicProvider getCluster();

}


