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

/** A Component which contains other components.
 * <p>
 * add, remove, etc will generally take either Component or 
 * ComponentDescription instances, depending on the Container.
 * <p>
 * ContainingComponent is similar to BeanContext.
 * <p>
 * The Container will implement or delegate to an implementation of 
 * a ContainerAPI callable by associated Binders (and BinderFactories).
 * In turn, any Container may invoke a required BinderAPI on any of its
 * associated Binders.
 *
 * @see java.beans.beancontext.BeanContext
 **/
public interface Container extends Component, Collection, StateObject
{
}
