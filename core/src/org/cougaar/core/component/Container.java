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
 * @see java.beans.beancontext.BeanContext
 **/
public interface Container extends Component, Collection
{
}
