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

/** ServiceRevokedEvent indicates that a service has been 
 * revoked in a service context.
 * @see java.beans.beancontext.BeanContextServiceRevokedEvent
 **/
public class ServiceRevokedEvent extends ServiceEvent {
  /** Construct a ServiceRevokedEvent **/
  ServiceRevokedEvent(ServiceBroker servicebroker, Class revokedService) {
    super(servicebroker, revokedService);
  }
}
