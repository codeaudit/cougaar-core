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
 **/
public abstract class BinderSupport implements Binder
{
  private ServiceBroker servicebroker;
  private ContainerAPI parent;
  private Component child;

  protected BinderSupport(ContainerAPI parent, Component child) {
    this.servicebroker = parent.getServiceBroker();
    this.parent = parent;
    this.child = child;
  }

  public ServiceBroker getServiceBroker() { return servicebroker; }
  public void requestStop() { 
    parent.remove(child);
  }
  protected final ContainerAPI getContainer() {
    return parent;
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
  
  /** Call (once) from subclass
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
  protected void initializeChild() {
    Class childClass = child.getClass();

    BindingSite proxy = getBinderProxy();
    try {
      Method m = childClass.getMethod("setBindingSite", new Class[]{BindingSite.class});
      if (m != null) {          // use a non-throwing variation in the future
        m.invoke(child, new Object[]{proxy});
      } 
    } catch (Exception e) {
      //e.printStackTrace();
      // ignore - maybe they'll use initialize(BindingSite) or maybe they don't
      // care.
    }

    if (servicebroker != null) {
      try {
        Method[] methods = childClass.getMethods();

        int l = methods.length;
        for (int i=0; i<l; i++) { // look at all the methods
          Method m = methods[i];
          String s = m.getName();
          if ("setBindingSite".equals(s)) continue;
          Class[] params = m.getParameterTypes();
          if (s.startsWith("set") &&
              params.length == 1) {
            Class p = params[0];
            if (Service.class.isAssignableFrom(p)) {
              String pname = p.getName();
              {                     // trim the package off the classname
                int dot = pname.lastIndexOf(".");
                if (dot>-1) pname = pname.substring(dot+1);
              }
            
              if (s.endsWith(pname)) {
                // ok: m is a "public setX(X)" method where X is a Service.
                // Let's try getting the service...
                Object service = servicebroker.getService(child, p, null);
                Object[] args = new Object[] { service };
                try {
                  m.invoke(child, args);
                } catch (InvocationTargetException ite) {
                  ite.printStackTrace();
                }
              }
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException(e.toString());
      }
    }

    // now call child.initialize, if there.
    try {
      Method init = null;
      try {
        init = childClass.getMethod("initialize", new Class[]{BindingSite.class});
      } catch (NoSuchMethodException e1) { }
      if (init != null) {
        init.invoke(child, new Object[] {proxy});
        // bail out!
        return;                 
      }
    } catch (Exception e) {
      e.printStackTrace();
      // no initialize(Binder - oh well, fall through.
    }

    try {
      Method init = null;
      try {
        init = childClass.getMethod("initialize", null);
      } catch (NoSuchMethodException e1) { }
      if (init != null) {
        init.invoke(child, new Object[] {});
      }
    } catch (Exception e) {
      // no initialize!  strange, but maybe it doesn't need it.
    }
    // all done.
  }
}
