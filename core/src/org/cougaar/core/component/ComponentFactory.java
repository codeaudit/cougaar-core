/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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
      throw new ComponentFactoryException("Couldn't load component class", desc, e);
    }
  }

  private final static Class[] VO = new Class[]{Object.class};

  /** Override to change how a class is constructed.  Should return 
   * a Component in order for the rest of the default code to work.
   **/
  protected Object instantiateClass(Class cc) {
    System.err.println("MIK: instantiateClass should not be called!!!  Please report this message to bugzilla");
    try {
      return cc.newInstance();
    } catch (Exception e) {
      throw new ComponentFactoryException("Couldn't instantiate class", null, e);
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
          //if (!((Collection)p).isEmpty()) ...
          Method m = cc.getMethod("setParameter", VO);
          m.invoke(o, new Object[]{p});
	}
        return (Component) o;
      } else {
        throw new IllegalArgumentException("ComponentDescription "+desc+" does not name a Component");
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
