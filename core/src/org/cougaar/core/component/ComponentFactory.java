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
import java.lang.reflect.Method;

/** An base class useful for creating components
 * and instilling the "breath of life" (initial services)
 * on behalf of manager objects.
 **/
public abstract class ComponentFactory
{
  /** try loading the class - may be overridden to check before loading **/
  protected Class loadClass(ComponentDescription desc) 
    throws ComponentFactoryException
  {
    try {
      return Class.forName(desc.getClassname());
    } catch (Exception e) {
      e.printStackTrace();
      throw new ComponentFactoryException("loadClass failure:", desc, e);
    }
  }

  private final static Class[] VO = new Class[]{Object.class};

  /** Override to change how a class is constructed.  Should return 
   * a Component in order for the rest of the default code to work.
   **/
  protected Object instantiateClass(Class cc) {
    try {
      return cc.newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /** instantiate the class - override to check the class before instantiation. **/
  protected Component instantiateComponent(ComponentDescription desc, Class cc)
    throws ComponentFactoryException
  {
    try {
      Object o = cc.newInstance();
      if (o instanceof Component) {
        Object p = desc.getParameter();
        if (p != null) {
	  if (!((Collection)p).isEmpty()) {
	    try {
	      Method m = cc.getMethod("setParameter", VO);
	      m.invoke(o, new Object[]{p});
	    } catch (Exception e) {
	      e.printStackTrace();
	      throw new ComponentFactoryException("Failed while setting parameter", desc, e);
	    }
	  }
	}
        return (Component) o;
      } else {
        throw new ComponentFactoryException("ComponentDescription does not name a Component", desc);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new ComponentFactoryException("Component cannot be instantiated", desc, e);
    }
  }
  
  /** Create an inactive component from a description object **/
  public Component createComponent(ComponentDescription desc) 
    throws ComponentFactoryException
  {
    return instantiateComponent(desc, loadClass(desc));
  }

  private static final ComponentFactory singleton = new ComponentFactory() {};
  public static final ComponentFactory getInstance() {
    return singleton;
  }
}
