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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;

/**
 * Write access to the society topology, which is reserved
 * for agent use only.
 */
public interface TopologyWriterService extends Service {

  /**
   * Create a topology entry for a new agent.
   * <p>
   * This verifies that the topology doesn't already contain 
   * an entry for the agent, or that the prior entry is
   * marked as "dead" and can be overwritten.
   *
   * @param type a TopologyEntry "*_TYPE" constant
   * @param newStatus a TopologyEntry status constant, such
   *    as "ACTIVE".
   */
  void createAgent(
      String agent, 
      int type,
      long newIncarnation, 
      long newMoveId,
      int newStatus);

  /**
   * Update the topology entry for a running or moving 
   * agent.
   * <p>
   * This verifies that the topology still contains the 
   * given asserted incarnation and move numbers, in 
   * case another copy of this agent was restarted.
   */
  void updateAgent(
      String agent, 
      int assertType,
      long assertIncarnation, 
      long newMoveId,
      int newStatus,
      long assertMoveId);

  /**
   * Remove the entry for the given agent.
   */
  void removeAgent(String agent);

}
