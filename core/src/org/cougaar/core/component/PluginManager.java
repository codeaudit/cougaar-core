/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/** A container for Plugin Components.
 **/
public class PluginManager 
  extends ContainerSupport
{

  public PluginManager() {
    if (!loadComponent(new PluginBinderFactory())) {
      throw new RuntimeException("Failed to load the PluginBinderFactory");
    }
  }

  protected ComponentFactory specifyComponentFactory() {
    return super.specifyComponentFactory();
  }
  protected String specifyContainmentPoint() {
    return "agent.plugin";
  }
  protected Services specifyChildContext() {
    return new PluginServices();
  }

  protected Class specifyChildBindingSite() {
    return PluginBindingSite.class;
  }

  protected Object getBinderFactoryProxy() {
    return this;
  }

  //
  // implement the API needed by plugin binders
  //

  /** Makes the child services available to child binders.
   * should use package protection to give access only to PluginBinderSupport,
   * but makes it public for use by Test example.
   **/
  public final Services getChildContext() {
    return childContext;
  }

  //
  // support classes
  //

  private static class PluginServices extends ServicesSupport {
  }
  
}
