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

/** Cougaar component Service Broker.
 * Note that this was previously called Services in deference to
 * the analogous BeanContextServices object.
 *
 * @see java.beans.beancontext.BeanContextServices 
 **/
public interface ServiceBroker {
  /** add a ServiceListener to this Services Context **/
  void addServiceListener(ServiceListener sl);
  /** remove a services listener **/
  void removeServiceListener(ServiceListener sl);

  /** add a Service to this Services Context.
   * @return true IFF successful and not redundant.
   **/
  boolean addService(Class serviceClass, ServiceProvider serviceProvider);
  /** remoke or remove an existing service **/
  void revokeService(Class serviceClass, ServiceProvider serviceProvider);

  /** is the service currently available? **/
  boolean hasService(Class serviceClass);

  /** gets the currently available services for this context **/
  Iterator getCurrentServiceClasses();

  /** get an instance of the requested service from a service provider associated
   * with this context.
   **/
  Object getService(Object requestor, Class serviceClass, ServiceRevokedListener srl);

  void releaseService(Object requestor, Class serviceClass, Object service);
}
