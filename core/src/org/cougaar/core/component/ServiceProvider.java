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

/** Like BeanContextServiceProvider **/
public interface ServiceProvider 
{
  /** @param serviceClass a Class, usually an interface, which extends Service. **/
  Object getService(ServiceBroker sb, Object requestor, Class serviceClass);

  void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service);
}
