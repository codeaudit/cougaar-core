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

/** Service event is a base class for all Service, Binder and Component 
 * Model Events, analogous.
 * @see java.beans.beancontext.BeanContextEvent
 **/
public abstract class ComponentModelEvent extends EventObject {
  public ComponentModelEvent(Object source) {
    super(source);
  }

}
