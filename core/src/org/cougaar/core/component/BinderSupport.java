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
  private Services services;
  private Container parentComponent;
  private Component childComponent;

  protected BinderSupport(Services services, Container parent, Component child) {
    this.services = services;
    this.parentComponent = parent;
    this.childComponent = child;
  }

  public Services getServices() { return services; }
  public void requestStop() { 
    if (parentComponent == null) {
      throw new IllegalArgumentException("Cannot stop this Component.");
    } else {
      parentComponent.remove(childComponent);
    }
  }
  protected final Container getParentComponent() {
    return parentComponent;
  }
  protected final Component getChildComponent() {
    return childComponent;
  }

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
   * if there was no setBinder and no initialize(Binder) methods because
   * the component might get everything it needs from services (or might not
   * need anything for some reason).
   * <p>
   * Often, the
   * child.initialize() method will call back into the services api.
   */
  protected void initializeChild() {
    Class childClass = childComponent.getClass();

    try {
      Method m = childClass.getMethod("setBindingSite", new Class[]{BindingSite.class});
      if (m != null) {          // use a non-throwing variation in the future
        m.invoke(childComponent, new Object[]{this});
      }
    } catch (Exception e) {
      //e.printStackTrace();
      // ignore - maybe they'll use initialize(Binder) or maybe they don't
      // care.
    }

    if (services != null) {
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
                Object service = services.getService(childComponent, p, null);
                Object[] args = new Object[] { service };
                try {
                  m.invoke(childComponent, args);
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
      Method init = childClass.getMethod("initialize", new Class[]{Binder.class});
      init.invoke(childComponent, new Object[] {this});
      return;                  
      // bail out!
    } catch (Exception e) {
      // no initialize(Binder - oh well, fall through.
    }

    try {
      Method init = childClass.getMethod("initialize", null);
      init.invoke(childComponent, new Object[] {});
    } catch (Exception e) {
      // no initialize!  strange, but maybe it doesn't need it.
    }
    // all done.
  }
}
