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
 * A BinderFactory provides Binder instances
 * on behalf of a ContainingComponent or ServiceProvider to wrap
 * Child components.
 * <p>
 * BinderFactories are generally themselves specially bound by only by
 * a trivial binder, so the "parent" component link is supplied shortly after
 * construction by a call to the setContainer method.  This may change
 * to something closer to the way that any other component is bound at 
 * some point.
 **/
public interface BinderFactory extends Component
{
  /** Lowest-priority for a BinderFactory.  Default infrastructure
   * BinderFactories are generally at this level.
   **/
  public final static int MIN_PRIORITY = 0;
  /** Typical intermediate priority for "real-world" BinderFactories.
   * Higher priorities should be used mainly for BinderFactoryWrappers.
   **/
  public final static int NORM_PRIORITY = 50;
  /** Highest priority, for use by very important specific
   * BinderFactoryWrappers.
   **/
  public final static int MAX_PRIORITY = 100;

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

  /** Get or Construct a binder for a child component.
   * The returned value will be a Binder
   * customized for use by the child Component.  The
   * Factory may impose any restrictions deemed neccessary on
   * the client object.  Most commonly, the child will often
   * be required to implement a client-side interface of a
   * service protocol.
   **/
  Binder getBinder(Object child);

  public final static class BFComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      return ((BinderFactory)o2).getPriority() - ((BinderFactory)o1).getPriority();
    }
  }

  /** a comparator for keeping Binder Factories sorted **/
  final static Comparator comparator = new BFComparator();

  /** Get the BinderFactory's ComponentFactory.  
   * May return null if the BinderFactory doesn't make components.
   **/
  ComponentFactory getComponentFactory();
}

