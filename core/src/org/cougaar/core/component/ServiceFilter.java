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
 * A BinderFactoryWrapper which wraps components using a Binder which 
 * examines and, optionally, modifies, wraps, or audits service requests.
 * Although instantiable, it is really only useful as a base class for
 * a Binder which really does Service Filtering and, perhaps, as an example 
 * of how to write Binders and BinderFactories.
 **/
public class ServiceFilter 
  extends BinderFactorySupport 
  implements BinderFactoryWrapper // indicate that this is a wrapper
{
  public ServiceFilter() {}

  protected Class getBinderClass(Object child) {
    return ServiceFilterBinder.class;
  }

  public int getPriority() { return NORM_PRIORITY; }

  /** standard getBinder implementation essentially calls getBinderClass and
   * then bindChild.
   **/
  public Binder getBinder(Class bindingSite, Object child) {
    // figure out which binder to use.
    Class bc = getBinderClass(child);
    if (bc == null) return null;

    return bindChild(bc, child);
  }
}
