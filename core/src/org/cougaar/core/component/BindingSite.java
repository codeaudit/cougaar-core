/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/** A BindingSite class names and specifies a specific parent Component
 * (Container) to child Component relationship API, e.g. the API of a 
 * parent which a child may invoke.
 **/
public interface BindingSite  // extends Service
{
  //
  // service request API
  //

  /** request access to the ServiceBroker layer of the parent.
   * Often, services will be attached using introspection at load time
   * but this accessor allows much more dynamic access to the 
   * service layer.
   **/
  ServiceBroker getServiceBroker();

  //
  // containment API
  //

  /** A Component may call this method to request that it be stopped **/
  void requestStop();

  // more component-to-parent requests go here
}
