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

/** Service event is a base class for all Service Events **/
public abstract class ServiceEvent extends ComponentModelEvent {
  public ServiceEvent(ServiceBroker sb) {
    super(sb);
  }

  /** @return the source object as a ServiceBroker typed instance **/
  public final ServiceBroker getServiceBroker() { return (ServiceBroker) getSource(); }

}
