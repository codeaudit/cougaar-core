/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/**
 * Implement the basics of a BinderFactory.  A full implementation
 * will at least implement the getBinderClass() method and may override getBinder.
 * We expect many BinderFactory implementations to not use this base
 * class at all and write full implementations themselves.
 * <p>
 * The default implementation does not implement setParameter or request any services.
 **/
public abstract class BinderFactorySupport implements BinderFactory
{

  private Object parentComponent = null;
  public final void setParentComponent(Object parent) {
    this.parentComponent = parent;
  }
  /** get a handle on the parent component/container of this BinderFactory **/
  protected final Object getParentComponent() {
    return parentComponent;
  }

  // 
  /** define/override to choose the class of the Binder to use.
   * This method should return null if the child is not bindable with
   * this Factory.
   **/
  public abstract Class getBinderClass(Object child);
  
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
   * @return A Binder instance of class bindingSite which is binding 
   * the child component or null.
   **/
  public abstract Binder bindChild(Class binderClass, Object child);

  // implement Component
  public BinderFactorySupport() {}
  //public void setParameter(Object parameter) { }

  // implement BinderFactory
  /** override to set a higher priority.  The default is MIN_PRIORITY (lowest) **/
  public int getPriority() { return MIN_PRIORITY; }

  /** standard getBinder implementation essentially calls getBinderClass and
   * then bindChild.
   **/
  public Binder getBinder(Class bindingSite, Object child) {
    // figure out which binder to use.
    Class bc = getBinderClass(bindingSite);
    if (bc == null) return null;

    return bindChild(bc, child);
  }
}
