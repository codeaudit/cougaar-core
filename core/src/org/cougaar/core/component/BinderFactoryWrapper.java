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

/** An idea for the future:
 * A BinderFactory which encapsulates all BinderFactories known
 * to a component of a lower priority.  Introduces a hierarchy
 * of BinderFactories rather than a prioritized list.
 * <p>
 * When a BinderFactoryWrapper is called, the argument may either
 * be the component to be bound (if nobody else bound it), or
 * a lower-tier Binder.
 **/
public interface BinderFactoryWrapper
  extends BinderFactory
{
}
