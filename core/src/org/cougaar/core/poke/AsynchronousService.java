/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.poke;

import org.cougaar.core.component.Component;
import org.cougaar.core.component.Service;

/** A Service that does its work for a client out of band.
 *
 **/

public interface AsynchronousService
  extends Service
{
  /**
   * AsyncrhonousService should call pc.poke when it has completed a job for a client
   * 
   * 
   **/
  void setPokable(Component comp, Pokable pc);
}
