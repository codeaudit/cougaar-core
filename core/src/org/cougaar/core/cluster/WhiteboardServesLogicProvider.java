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

import org.cougaar.util.UnaryPredicate;

import java.util.Enumeration;
import java.util.Collection;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.society.UID;

public interface WhiteboardServesLogicProvider
{
  /** Apply predicate against the entire "Whiteboard".
   * User provided predicate
   **/
  Enumeration searchWhiteboard(UnaryPredicate predicate);

  /** Add Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  void add(Object o);

  /** Removed Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  void remove(Object o);

  /** Change Object to the LogPlan Collection
   * (All subscribers will be notified)
   **/
  void change(Object o, Collection changes);

  /**
   * Alias for sendDirective(dir, null);
   **/
  void sendDirective(Directive dir);

  /**
   * Reliably send a directive. Take pains to retransmit this message
   * until it is acknowledged even if clusters crash.
   **/
  void sendDirective(Directive dir, Collection changeReports);

  PublishHistory getHistory();

}

