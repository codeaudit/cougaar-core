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
