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

  /** Select the binder to use.
   **/
  public Class getBinderClass(Object child) {
    return DefaultPluginBinder.class;
  }
  
  /** Bind the Child component.  <p>
   * The child component will already have been instantiated and any
   * parameter has been set.  Depending on the ComponentFactory (or other
   * constructor/initializer methods) used, there may have been additional
   * initialization performed. <p>
   * Generally all this method does is construct a new instance of 
   * bindingSite for use with the child component.
   * Various implementations may do additional Binder initialization
   * such as starting a thread, instructing the binder to provide additional
   * services, etc.
   *
   * @return A Binder instance of class bindingSite which is binding 
   * the child component or null.
   **/
  public Binder bindChild(Class binderClass, Object child) {
    PluginManagerForBinder pi = (PluginManagerForBinder) getParentComponent();
    try {
      Constructor constructor = binderClass.getConstructor(new Class[]{Object.class, Component.class});
      Binder binder = (Binder) constructor.newInstance(new Object[] {pi, child});
      ((DefaultPluginBinder)binder).initialize();
      return binder;
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }
}
