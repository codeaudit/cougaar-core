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

/** ServiceAvailableEvent indicates that a new service is available
 * to clients of a ServiceBroker
 * @see java.beans.beancontext.BeanContextServiceAvailableEvent
 **/
public class ServiceAvailableEvent extends ServiceEvent {
  private Class service;

  /** Construct a ServiceAvailableEvent **/
  public ServiceAvailableEvent(ServiceBroker sb, Class service) {
    super(sb);
    this.service = service;
  }

  /** @return the binding site newly available **/
  public final Class getService() { return service; }
}
