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

import org.cougaar.domain.planning.ldm.plan.Directive;
import java.util.Collection;

/** Marker interface indicating that the Logic Provider is for
 * handling Directives rather than transaction Envelopes.
 **/

public interface MessageLogicProvider extends LogicProvider {

  /** Called by LogPlan on each received Message.
   * @return true iff it actually performed an action based on the 
   * message.
   **/
  void execute(Directive m, Collection changeReports);
}
