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

/** Like BeanContextServicesListener **/
public interface ServiceAvailableListener extends ServiceListener {
  void serviceAvailable(ServiceAvailableEvent ae);
}
