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
import java.lang.reflect.*;

/** A Shell implementation of a Binder which does introspection-based
 * initialization and hooks for startup of the child component.
 * <p>
 * Note that the child is likely to still be a ComponentDescription object at
 * Binder Construction time.
 **/
public abstract class BinderSupport 
  extends BinderBase
{
  private ComponentDescription childD;
  private Component child;

  protected BinderSupport(BinderFactory bf, Object childX) {
    super(bf, childX);
  }

  protected void attachChild(Object cd) {
    if (cd instanceof ComponentDescription) {
      childD = (ComponentDescription) cd;
      child = null;
    } else if (cd instanceof Component) {
      childD = null;
      child = (Component) cd;
    } else {
      throw new IllegalArgumentException("Child is neither a ComponentDescription nor a Component: "+cd);
    }
  }

  protected Component constructChild() {
    if (child != null) return child;
    ComponentFactory cf = getComponentFactory();
    if (cf == null) {
      throw new RuntimeException("No ComponentFactory, so cannot construct child component!");
    }
    if (childD == null) {
      throw new RuntimeException("No valid ComponentDescription.");
    }
      
    try {
      return cf.createComponent(childD);
    } catch (ComponentFactoryException cfe) {
      cfe.printStackTrace();
      throw new RuntimeException("Failed to construct child: "+cfe);
    }
  }

  // implement the BindingSite api

  public void requestStop() { 
    if (child != null)
      getContainer().remove(child);
  }

  protected final Component getComponent() {
    return child;
  }

  /** Defines a pass-through insulation layer to ensure that the plugin cannot 
   * downcast the BindingSite to the Binder and gain control via introspection
   * and/or knowledge of the Binder class.  This is neccessary when Binders do
   * not have private channels of communication to the Container.
   **/
  protected abstract BindingSite getBinderProxy();

  //
  // child services initialization
  //
  
  /** Called
   * to hook up all the requested services for the child component.
   * <p>
   * Initialization steps:
   * 1. call child.setBindingSite(BindingSite) if defined.
   * 2. uses introspection to find and call child.setService(X) methods where 
   * X is a Service.  All such setters are called, even if the service
   * is not found.  If a null answer is not acceptable, the component
   * should throw a RuntimeException.
   * 3. call then calls child.initialize(Binder) if defined - if not defined
   * call child.initialize() (if defined).  We do not error or complain even
   * if there was no setBinder and no initialize(BindingSite) methods because
   * the component might get everything it needs from services (or might not
   * need anything for some reason).
   * <p>
   * Often, the
   * child.initialize() method will call back into the services api.
   */
  public void initialize() {
    if (child == null) {
      child = constructChild();
    }

    BindingSite proxy = getBinderProxy();
    BindingUtility.setBindingSite(child, proxy);
    if (getServiceBroker() != null) {
      BindingUtility.setServices(child, getServiceBroker());
    } else {
      System.err.println("BinderSupport: No ServiceBroker from "+getContainer()+" for "+child);
      Thread.dumpStack();
    }

    // cascade
    child.initialize();
  }
  public void load() {
    child.load();
  }
  public void start() {
    child.start();
  }

  public Object getState() {
    if (child instanceof StateObject) {
      return ((StateObject)child).getState();
    } else {
      return null;
    }
  }

  public void setState(Object state) {
    if (child instanceof StateObject) {
      ((StateObject)child).setState(state);
    }
  }
}
