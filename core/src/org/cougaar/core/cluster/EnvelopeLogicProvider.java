/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import java.util.Collection;

/** API for LogPlan LogicProviders which handle transaction packets
 * (EnvelopeTuples) rather than Messages.
 **/

public interface EnvelopeLogicProvider extends LogicProvider {
  /** Called by LogPlan on each received EnvelopeTuple.
   * @return true iff it actually performed an action based on the 
   * tuple.
   **/
  void execute(EnvelopeTuple m, Collection changeReports);
}
