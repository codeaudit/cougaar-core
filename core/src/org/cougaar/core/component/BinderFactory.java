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

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/**
 * A BinderFactory provides Binder instances
 * on behalf of a ContainingComponent or ServiceProvider to wrap
 * Components or ServiceConsumers.
 * It may be a component (so that is can be dynamically plugged in) but is not
 * required to be.
 **/
public interface BinderFactory 
{
  public final static int MIN_PRIORITY = 1;
  public final static int NORM_PRIORITY = 5;
  public final static int MAX_PRIORITY = 10;

  /** The priority of this binder factory.  The range of values
   * is specified by the MIN_PRIORITY to MAX_PRIORITY.
   * This value determines stacking order, with MAX_PRIORITY factories 
   * getting first chance to construct binders.  Generally,
   * the first one to respond wins.
   * <p>
   * This is pretty half-baked, but we need some policy to resolve
   * ambiguity.  Perhaps a two-level approach would be sufficient,
   * e.g. built-in versus dynamically loaded factory.  Also an
   * option is to logically try all available binder factories and
   * then resolve any conflicts which might arise.
   **/
  int getPriority();

  /** Get or Construct a binder for a client which conforms to a
   * specific service class.  The returned value will be a Binder
   * (e.g. an instance of a class which implements the serviceClass
   * BindingSite) customized for use by the client object.  The
   * Factory may impose any restrictions deemed neccessary on
   * the client object.  Most commonly, the client will often
   * be required to implement a client-side interface of the 
   * service protocol.
   **/
  Binder getBinder(Class serviceClass, Object client);
}

