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
import java.lang.reflect.*;
import org.cougaar.core.component.*;

/**
 * A simple BinderFactory for binding domain Plugins.
 **/
public class DefaultPluginBinderFactory extends BinderFactorySupport
{

  /** Select the binder to use - must be an extension of DefaultPluginBinder.
   **/
  protected Class getBinderClass(Object child) {
    return DefaultPluginBinder.class;
  }
  
  /** Bind a plugin with a plugin binder. 
   **/
  protected Binder bindChild(Class binderClass, Object child) {
    Binder b = super.bindChild(binderClass, child);
    if (b == null) {
      return null;
    } else {
      if (b instanceof DefaultPluginBinder) {
        return b;
      } else {
        System.err.println("Illegal binder class specified: "+binderClass);
        return null;
      }
    }
  }


  private final static ComponentFactory pluginCF = new PluginComponentFactory();
  public final ComponentFactory getComponentFactory() {
    return pluginCF;
  }

  protected static class PluginComponentFactory 
    extends ComponentFactory 
  {
    protected Object instantiateClass(Class cc) {
      Object o;
      if (PlugIn.class.isAssignableFrom(cc)) {
        o = new StatelessPlugInAdapter(getPurePlugIn(cc));
      } else {
        o = super.instantiateClass(cc);
      }
      return o;
    }
  }

  //
  // class hackery for old-style pure plugin caching
  //

  private static final HashMap purePlugIns = new HashMap(11);
  private static PlugIn getPurePlugIn(Class c) {
    synchronized (purePlugIns) {
      PlugIn plugin = (PlugIn)purePlugIns.get(c);
      if (plugin == null) {
        try {
          plugin = (PlugIn) c.newInstance();
          purePlugIns.put(c, plugin);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      return plugin;
    }
  }

}
