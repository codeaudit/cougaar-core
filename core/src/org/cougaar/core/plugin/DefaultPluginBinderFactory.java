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
  
  /** Bind a plugin with a plugin binder.  Calls initialize 
   * after constructing the binder.
   **/
  protected Binder bindChild(Class binderClass, Object child) {
    Binder b = super.bindChild(binderClass, child);
    if (b == null) {
      return null;
    } else {
      if (b instanceof DefaultPluginBinder) {
        ((DefaultPluginBinder)b).initialize();
        return b;
      } else {
        System.err.println("Illegal binder class specified: "+binderClass);
        return null;
      }
    }
  }
}
