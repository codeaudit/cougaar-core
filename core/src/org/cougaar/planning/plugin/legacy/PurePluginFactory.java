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

package org.cougaar.planning.plugin.legacy;

import java.util.HashMap;
import org.cougaar.core.component.ComponentFactory;

/**
 * A simple ComponentFactory for creating Plugin instances.
 */
public class PurePluginFactory extends ComponentFactory
{

  protected Object instantiateClass(Class cc) {
    Object o;
    if (Plugin.class.isAssignableFrom(cc)) {
      o = new StatelessPluginAdapter(getPurePlugin(cc));
    } else {
      o = super.instantiateClass(cc);
    }
    return o;
  }

  //
  // class hackery for old-style pure plugin caching
  //

  private static final HashMap purePlugins = new HashMap(11);
  private static Plugin getPurePlugin(Class c) {
    synchronized (purePlugins) {
      Plugin plugin = (Plugin)purePlugins.get(c);
      if (plugin == null) {
        try {
          plugin = (Plugin) c.newInstance();
          purePlugins.put(c, plugin);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return plugin;
    }
  }

}
