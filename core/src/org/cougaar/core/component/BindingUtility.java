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

/** A collection of utilities to be used for binding components
 **/

public abstract class BindingUtility {

  public static boolean activate(Object child, BindingSite bindingSite, ServiceBroker serviceBroker) {
    setBindingSite(child, bindingSite);
    setServices(child, serviceBroker);
    initialize(child);
    load(child);
    start(child);
    return true;
  }

  /** Sets a the binding site of the child to the specified object
   * if possible.
   * @return true on success
   **/
  public static boolean setBindingSite(Object child, BindingSite bindingSite) { 
    Class childClass = child.getClass();
    try {
      Method m;
      try {
        m = childClass.getMethod("setBindingSite", new Class[]{BindingSite.class});
      } catch (NoSuchMethodException e) {
        return false;
      }

      if (m != null) {          // use a non-throwing variation in the future
        //System.err.println("Invoking "+child+".setBindingSite("+bindingSite+")");
        m.invoke(child, new Object[]{bindingSite});
        return true;
      } 
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public static boolean setServices(Object child, ServiceBroker servicebroker) {
    Class childClass = child.getClass();
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
    return true;
  }

  /** Run initialize on the child component if possible **/
  public static boolean initialize(Object child) { 
    return call0(child, "initialize");
  }

  /** Run load on the child component if possible **/
  public static boolean load(Object child) { 
    return call0(child, "load");
  }

  /** Run start on the child component if possible **/
  public static boolean start(Object child) { 
    return call0(child, "start");
  }

  public static boolean call0(Object child, String method) {
    Class childClass = child.getClass();
    try {
      Method init = null;
      try {
        init = childClass.getMethod(method, null);
      } catch (NoSuchMethodException e1) { }
      if (init != null) {
        init.invoke(child, new Object[] {});
        //System.err.println("Invoked "+method+" on "+child);
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      //throw e;
    }
    return false;
  }


}
