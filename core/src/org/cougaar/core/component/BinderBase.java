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
