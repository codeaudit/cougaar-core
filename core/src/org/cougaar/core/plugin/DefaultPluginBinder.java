/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ContainedBinderSupport;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.util.ConfigFinder;

/** The standard Binder for Plugins.
 **/
public class DefaultPluginBinder 
  extends ContainedBinderSupport // publish the ContainedService to the subcomponent
  implements PluginBinder
{
  /** All subclasses must implement a matching constructor. **/
  public DefaultPluginBinder(BinderFactory bf, Object child) {
    super(bf, child);
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
    public final MessageAddress getAgentIdentifier() {
      return (MessageAddress) getPluginManager().getAgentIdentifier();
    }
    public final ConfigFinder getConfigFinder() {
      return getPluginManager().getConfigFinder();
    }
    public String toString() {
      return "Proxy for "+(DefaultPluginBinder.this.toString());
    }
  }

  public String toString() {
    return (super.toString())+"/"+getComponent();
  }

  /** useful shorthand for binder functions **/
  protected final MessageAddress getAgentIdentifier() {
    return getPluginManager().getAgentIdentifier();
  }


}
