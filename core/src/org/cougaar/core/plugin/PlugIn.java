/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.*;
import org.cougaar.core.cluster.ClusterIdentifier;

import org.cougaar.util.UnaryPredicate;
import java.util.*;

import org.cougaar.core.component.Component;

/** 
 * This is the basic class required for
 * implementing a "stateless" plugin.  Extending
 * classes should avoid any data members - any
 * plugin-instance specific state should be
 * stored in an implementation of PlugIn.State.
 *
 * Only a single instance of each class of PlugIn
 * will be constructed <em>per VM</em>.
 **/
public abstract class PlugIn
{
  /** Infrastructure calls to allow the plugin
   * to contact the cluster, setup subscriptions,
   * recover from rehydration, etc.
   *
   * A critical step is for the plugin to call
   * support.setPlugInState(PlugInState) so
   * that it can retrieve any desired state (e.g.
   * subscriptions, etc) in later invocations.
   * 
   * If the plugin does not supply this method,
   * the default will be that the plugin not
   * get a PlugIn.State.
   *
   * initialize will always be called inside 
   * a transaction.
   **/
  protected void initialize(PlugInContext support) {
  }

  /**
   * execute is called by the infrastructure
   * any time the plugin should run.  It will
   * never be called in a context when initialize
   * has not run.
   *
   * execute will always be called inside 
   * a transaction.
   */
  protected void execute(PlugInContext support) {
  }

  /** a PlugIn may add a single State instance
   * to a PlugInContext via the PlugInContext.setState
   * method during a call to initialize.
   * Thereafter, the plugin may retrieve the state
   * for use in subsequent execute cycles.  
   *
   * It is completely up to each plugin what appears in
   * its State instance.
   *
   * State implementations need not be serializable for
   * persistence or any other reason.
   **/
  public static interface State {
  }
}
    
  
  
