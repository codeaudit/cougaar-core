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
import org.cougaar.util.GenericStateModel;

/** A Component is the base class of the Component and
 * Service Model.  Components form a strict hierarchy
 * via Container/Contained relationships and may have
 * any number of additional (possibly mediated) client/server 
 * relationships.
 * <p>
 * A component must implement the childAPI required by it's container.
 * <p>
 * Construction of a component consists of the following steps:
 * 1. zero-argument constructor is called.
 * 2. if a ComponentDescription is being used to create the
 * component, and it specifies a non-null parameter, the optional
 * setParameter(Object) method is called with the parameter as the
 * argument.  A ComponentFactoryException will be raised if the
 * parameter is non-null, but no setParameter(Object) method is 
 * defined.
 * 3. The binder (if capable) will use introspection to 
 * find setX(X) methods where X is a known service in the Context.
 * 4. the binder will call the initialize(BindingSite x) method
 * where x is the binder chosen for this component.
 * <p>
 * Component is similar to BeanContextChild.
 * @see java.beans.beancontext.BeanContextChild
 **/
public interface Component 
  extends GenericStateModel
{
}
