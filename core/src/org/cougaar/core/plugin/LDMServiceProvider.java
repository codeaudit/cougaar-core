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
import org.cougaar.domain.planning.ldm.*;

/** placeholder to clean up plugin->manager interactions **/
public class LDMServiceProvider implements ServiceProvider
{
  private ClusterImpl agent;

  public LDMServiceProvider(ClusterImpl agent) {
    this.agent = agent;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (LDMService.class.isAssignableFrom(serviceClass)) {
      return new LDMServiceImpl();
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private class LDMServiceImpl implements LDMService {
    public LDMServesPlugIn getLDM() {
      return agent.getLDM();
    }
    public RootFactory getFactory() {
      return getLDM().getFactory();
    }
    public Factory getFactory(String s) {
      return getLDM().getFactory(s);
    }

    // standin API for LDMService called by PluginBinder for temporary support
    public void addPrototypeProvider(PrototypeProvider plugin) {
      agent.addPrototypeProvider(plugin);
    }
    public void addPropertyProvider(PropertyProvider plugin) {
      agent.addPropertyProvider(plugin);
    }
    public void addLatePropertyProvider(LatePropertyProvider plugin) {
      agent.addLatePropertyProvider(plugin);
    }
  }
}
  
