/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/** The standard Binder for Plugins.
 **/
public class PluginBinder extends BinderSupport implements PluginBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public PluginBinder(Object parentInterface, Component child) {
    super(((PluginManager) parentInterface).getChildContext(), 
          (PluginManager) parentInterface, 
          child);
  }

  /** package-private kickstart method for use by the PluginBinderFactory **/
  void initialize() {
    initializeChild();          // set up initial services
  }
}
