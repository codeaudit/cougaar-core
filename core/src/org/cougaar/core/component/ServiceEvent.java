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

/** Service event is a base class for ServicesEvents **/
public abstract class ServiceEvent extends ComponentModelEvent {
  public ServiceEvent(Services services) {
    super(services);
  }

  /** @return the source object as a Services typed instance **/
  public final Services getServices() { return (Services) getSource(); }

}
