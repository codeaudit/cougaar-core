/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.domain;

import org.cougaar.core.agent.ClusterIdentifier;

/** 
 * API for LogPlan LogicProviders which handle agent restarts
 */
public interface RestartLogicProvider extends LogicProvider {

  /**
   * Called by the LogPlan whenever this agent or a remote agent 
   * restarts.
   * <p>
   * The primary function of this API is to allow a logic providers 
   * to reconcile the state of restarted agents.
   * <p>
   * If the given "cid" is null then <i>this</i> agent has 
   * restarted.  This logic provider should resend/confirm its 
   * state with all (remote) agents that it has communicated 
   * with.
   * <p>
   * If the given "cid" is non-null then the "cid" is
   * for a remote agent that has been restarted.  This
   * logic provider should resend/confirm its state 
   * <i>with regards to that one remote agent</i>.
   *
   * @param cid null if this agent restarted, otherwise the
   *            ClusterIdentifier of a remote agent that restarted
   *
   * @see RestartLogicProviderHelper utility method to test the 
   *    given "cid"
   */
  void restart(ClusterIdentifier cid);
}







