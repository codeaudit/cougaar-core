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

/** BinderBase contains logic for the parent link (ContainerAPI) but not the 
 * child link.  BinderSupport extends the api for standard Binders and BinderWrapper
 * extends it for "Wrapping" Binders.
 **/
public abstract class BinderBase
  implements Binder
{
  private BinderFactory binderFactory;
  private ServiceBroker servicebroker;
  private ContainerAPI parent;

  protected BinderBase(BinderFactory bf, Object child) {
    binderFactory = bf;
    attachChild(child);
  }

  public void setBindingSite(BindingSite bs) {
    if (bs instanceof ContainerAPI) {
      parent = (ContainerAPI) bs;
      servicebroker = parent.getServiceBroker();
    } else {
      throw new RuntimeException("Help: BindingSite of Binder not a ContainerAPI!");
    }
  }

  protected abstract void attachChild(Object child);

  protected ComponentFactory getComponentFactory() {
    if (binderFactory != null) {
      return binderFactory.getComponentFactory();
    } else {
      throw new RuntimeException("No ComponentFactory");
    }
  }

  public ServiceBroker getServiceBroker() { return servicebroker; }

  protected final ContainerAPI getContainer() {
    return parent;
  }

  //
  // child services initialization
  //
  
  public abstract void initialize();
  public abstract void load();
  public abstract void start();
  public abstract void suspend();
  public abstract void resume();
  public abstract void stop();
  public abstract void halt();
  public abstract void unload();
  public abstract int getModelState();

  public abstract Object getState();
  public abstract void setState(Object state);

  public String toString() {
    String s = this.getClass().toString();
    int i = s.lastIndexOf(".");
    return s.substring(i+1);
  }

}
