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
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.UIDServer;

/** The standard Binder for Plugins.
 **/
public class DefaultPluginBinder 
  extends BinderSupport 
  implements PluginBinder, PluginBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public DefaultPluginBinder(Object parentInterface, Component child) {
    super((PluginManagerForBinder) parentInterface, 
          child);
  }

  /** package-private kickstart method for use by the PluginBinderFactory **/
  protected void initialize() {
    initializeChild();          // set up initial services
  }

  protected final PluginBase getPlugin() {
    return (PluginBase) getComponent();
  }
  protected final PluginManagerForBinder getPluginManager() {
    return (PluginManagerForBinder)getContainer();
  }


  //
  // cluster  
  // 

  public final ClusterIdentifier getAgentIdentifier() {
    return getPluginManager().getAgentIdentifier();
  }

  public final UIDServer getUIDServer() {
    return getPluginManager().getUIDServer();
  }

  public ConfigFinder getConfigFinder() {
    return getPluginManager().getConfigFinder();
  }

  public String toString() {
    return getPlugin() + "'s PluginBinder";
  }


}
