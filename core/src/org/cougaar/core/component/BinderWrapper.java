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

/** A base class for a BinderWrapper: A binder which is interposed between a container 
 * and another binder.
 **/
public abstract class BinderWrapper
  extends BinderBase
  implements ContainerAPI
{
  private Binder child;

  protected BinderWrapper(BinderFactory bf, Object childX) {
    super(bf, childX);
  }

  protected void attachChild(Object cd) {
    if (cd instanceof Binder) {
      child = (Binder) cd;
    } else {
      throw new IllegalArgumentException("Child is not a Binder: "+cd);
    }
  }

  protected final Binder getChildBinder() {
    return child;
  }

  // implement ContainerAPI

  /** Defines a pass-through insulation layer to ensure that lower-level binders cannot
   * downcast the ContainerAPI to the real BinderWrapper and gain additional 
   * privileges.  The default is to implement it as a not-very secure return
   * of the BinderWrapper itself.
   **/
  protected ContainerAPI getContainerProxy() {
    return this;
  }

  public boolean remove(Object childComponent) {
    return getContainer().remove(childComponent);
  }
  
  public void requestStop() {
    // ignore - this would be a request to stop the bind below, but the binder
    // child should actually be using the remove(Object) api by this point instead.
  }

  //
  // child services initialization
  //
  
  public void initialize() {
    ContainerAPI proxy = getContainerProxy();
    BindingUtility.setBindingSite(getChildBinder(), proxy);
    if (getServiceBroker() != null) {
      BindingUtility.setServices(getChildBinder(), getServiceBroker());
    } else {
      System.err.println("BinderWrapper: No ServiceBroker!");
    }
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
    return child.getState();
  }
  public void setState(Object state) {
    child.setState(state);
  }
}
