/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.component.*;

/**
 * A BinderFactory for binding PluginManagers to Agents.
 **/
public class PluginManagerBinderFactory extends BinderFactorySupport
{

  /** PluginManagerBinderFactory always uses PluginManagerBinder.
   **/
  public Class getBinderClass(Object child) {
    return PluginManagerBinder.class;
  }
  
}

