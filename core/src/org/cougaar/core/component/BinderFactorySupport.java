/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

/**
 * Implement the basics of a BinderFactory.  A full implementation
 * will at least implement the getBinderClass() method and may override getBinder.
 * We expect many BinderFactory implementations to not use this base
 * class at all and write full implementations themselves.
 * <p>
 * The default implementation does not implement setParameter or request any services.
 **/
public abstract class BinderFactorySupport 
  extends org.cougaar.util.GenericStateModelAdapter
  implements BinderFactory
{

  private BindingSite parentComponent = null;
  public void setBindingSite(BindingSite bs) { parentComponent = bs; }
  protected final BindingSite getBindingSite() { return parentComponent; }

  // 
  /** Override to choose the class of the Binder to use.
   * This method should return null if the child is not bindable with
   * this Factory.  The default implementation returns null.
   **/
  protected Class getBinderClass(Object child) { return null; }
  
  /** Bind the Child component.  <p>
   * The child component will already have been instantiated and any
   * parameter has been set.  Depending on the ComponentFactory (or other
   * constructor/initializer methods) used, there may have been additional
   * initialization performed. <p>
   * Generally all this method does is construct a new instance of 
   * bindingSite for use with the child component.
   * Various implementations may do additional Binder initialization
   * such as starting a thread, instructing the binder to provide additional
   * services, etc.
   *
   * By default, it does the equivalent of return new <em>binderClass</em>(ContainerAPI,child);
   *
   * @return A Binder instance of class bindingSite which is binding 
   * the child component or null.
   **/
  protected Binder bindChild(Class binderClass, Object child) {
    try {
      Constructor constructor = binderClass.getConstructor(new Class[]{BinderFactory.class, Object.class});
      Binder binder = (Binder) constructor.newInstance(new Object[] {this, child});
      return binder;
    } catch (Exception e) {
      System.err.println("Failed to construct "+binderClass+" to bind "+child+":");
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  // implement Component
  public BinderFactorySupport() {}
  //public void setParameter(Object parameter) { }

  // implement BinderFactory
  /** override to set a higher priority.  The default is MIN_PRIORITY (lowest) **/
  public int getPriority() { return MIN_PRIORITY; }

  /** standard getBinder implementation essentially calls getBinderClass and
   * then bindChild.
   **/
  public Binder getBinder(Object child) {
    // figure out which binder to use.
    Class bc = getBinderClass(child);
    if (bc == null) return null;

    return bindChild(bc, child);
  }

  /** Provide a default ComponentFactory instance.  This implementation
   * simply returns a default ComponentFactory static instance.  It should <em>not</em>
   * return a new instance each time.
   **/
  public ComponentFactory getComponentFactory() {
    return ComponentFactory.getInstance();
  }
}
