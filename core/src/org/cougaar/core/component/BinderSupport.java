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

  /** @throws ComponentFactoryException when it cannot be constructed.
   **/
  protected Component constructChild() {
    if (child != null) return child;
    ComponentFactory cf = getComponentFactory();
    if (cf == null) {
      throw new RuntimeException("No ComponentFactory, so cannot construct child component!");
    }
    if (childD == null) {
      throw new RuntimeException("No valid ComponentDescription.");
    }
      
    return cf.createComponent(childD);
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
  public void suspend() {
    child.suspend();
  }
  public void resume() {
    child.resume();
  }
  public void stop() {
    child.stop();
  }
  public void halt() {
    child.halt();
  }
  public void unload() {
    child.unload();
  }
  public int getModelState() {
    return child.getModelState();
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
    } else {
      System.err.println(
          "BinderSupport: No \"setState(..)\" from "+
          getContainer()+" for "+child);
      Thread.dumpStack();
    }
  }
}
