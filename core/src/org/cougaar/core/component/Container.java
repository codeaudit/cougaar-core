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

/** 
 * A Component which contains other components.
 * <p>
 * A Container plays a role similar to a BeanContext.
 * <p>
 * Most Collection operations (add, remove, contains, etc) expect 
 * either ComponentDescription or Component instances as arguments, 
 * depending upon the caller and this component's container.  In
 * general a ComponentDescription is preferred.
 * <p>
 * Like all components, this component has an implicit 
 * "insertion point" in the component hierarchy.  A collection 
 * operation that specifies a ComponentDescription at a lower-level
 * insertion point will be forwarded down the hierarchy to the
 * appropriate child container.
 * <p>
 * The collection API of a container is defined to be the recursive
 * set of all components contained in that container.  For example,
 * if this container contains a "sub-" container, the size of this
 * container includes all the components in that "sub-" container.
 * All collection methods are similarily defined.
 * <p>
 * The Container will implement or delegate to an implementation of 
 * a ContainerAPI callable by associated Binders (and BinderFactories).
 * In turn, any Container may invoke a required BinderAPI on any of its
 * associated Binders.
 *
 * @see java.beans.beancontext.BeanContext
 **/
public interface Container extends Component, Collection
{
}
