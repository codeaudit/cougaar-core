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

import org.cougaar.core.domain.*;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.domain.*;

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
  
