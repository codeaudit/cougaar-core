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
import java.lang.reflect.*;

/** The standard Binder for Plugins.
 **/
public class DefaultPluginBinder 
  extends BinderSupport 
  implements PluginBinder
{
  /** All subclasses must implement a matching constructor. **/
  public DefaultPluginBinder(Object parentInterface, Component child) {
    super((PluginManagerForBinder) parentInterface, 
          child);
  }

  /** package-private kickstart method for use by the PluginBinderFactory **/
  protected void initialize() {
    initializeChild();          // set up initial services

    // see if we need to run in compatability mode...
    Component child = getComponent();
    if (child instanceof PlugInServesCluster) { // old-style compatability until we do more porting
      PlugInServesCluster plugin = (PlugInServesCluster) child;
      plugin.load(null); // argument is ignored now
    }
  }

  protected final PluginBase getPlugin() {
    return (PluginBase) getComponent();
  }
  protected final PluginManagerForBinder getPluginManager() {
    return (PluginManagerForBinder)getContainer();
  }

  protected BindingSite getBinderProxy() {
    return new PluginBindingSiteImpl();
  }

  /** Implement the binding site delegate **/
  protected class PluginBindingSiteImpl implements PluginBindingSite {
    public final ServiceBroker getServiceBroker() {
      return DefaultPluginBinder.this.getServiceBroker();
    }
    public final void requestStop() {
      DefaultPluginBinder.this.requestStop();
    }
    public final ClusterIdentifier getAgentIdentifier() {
      return getPluginManager().getAgentIdentifier();
    }
    public final UIDServer getUIDServer() {
      return getPluginManager().getUIDServer();
    }
    public final ConfigFinder getConfigFinder() {
      return getPluginManager().getConfigFinder();
    }
    public String toString() {
      return "Proxy for "+(DefaultPluginBinder.this.toString());
    }
  }


  public String toString() {
    return (this.getClass().toString())+"/"+getPlugin();
  }

  /** useful shorthand for binder functions **/
  protected final ClusterIdentifier getAgentIdentifier() {
    return getPluginManager().getAgentIdentifier();
  }


}
