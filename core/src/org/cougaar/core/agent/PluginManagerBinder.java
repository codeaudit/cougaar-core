/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.agent.Agent;
import org.cougaar.core.cluster.*;
import org.cougaar.core.component.*;
import org.cougaar.core.plugin.PluginManager;

/** The standard Binder for PluginManagers and others attaching to an Agent.
 **/
public class PluginManagerBinder extends BinderSupport implements PluginManagerBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public PluginManagerBinder(BinderFactory parentInterface, Object child) {
    super(parentInterface, child);
  }

  /** package-private kickstart method for use by the PluginManagerBinderFactory **/
  public void initialize() {
    super.initialize();
    initializeChild();          // set up initial services
  }

  protected final PluginManager getPluginManager() {
    return (PluginManager) getComponent();
  }
  protected final Agent getAgent() {
    return (Agent)getContainer();
  }
  public ClusterIdentifier getAgentIdentifier() {
    return getAgent().getAgentIdentifier();
  }
  public UIDServer getUIDServer() {
    return getAgent().getUIDServer();
  }
  public ConfigFinder getConfigFinder() {
    return getAgent().getConfigFinder();
  }
  /** Defines a pass-through insulation layer to ensure that the plugin cannot 
   * downcast the BindingSite to the Binder and gain control via introspection
   * and/or knowledge of the Binder class.  This is neccessary when Binders do
   * not have private channels of communication to the Container.
   **/
  protected BindingSite getBinderProxy() {
    // do the right thing later
    return this;
  }

  public String toString() {
    return getPluginManager() + "'s PluginManagerBinder";
  }


}
