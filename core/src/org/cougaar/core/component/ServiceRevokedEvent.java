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

/** ServiceRevokedEvent indicates that a new service is available
 * in a given service context.
 * @see java.beans.beancontext.BeanContextServiceRevokedEvent
 **/
public class ServiceRevokedEvent extends ServiceEvent {
  private Class bindingSite;

  /** Construct a ServiceRevokedEvent **/
  ServiceRevokedEvent(Services services, Class bindingSite) {
    super(services);
    this.bindingSite = bindingSite;
  }

  /** @return the revoked BindingSite **/
  public final Class getBindingSite() { return bindingSite; }

}
