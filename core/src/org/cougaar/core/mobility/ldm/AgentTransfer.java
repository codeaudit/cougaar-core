/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.mobility.ldm;

import org.cougaar.core.component.StateTuple;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * For mobility infrastructure use only!.
 * <p>
 * Node-to-node agent transfer request.
 */
public interface AgentTransfer extends UniqueObject {

  int NO_STATUS = 0;
  int FAILURE_STATUS = 1;
  int SUCCESS_STATUS = 2;

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Get the optional UID of the object that "owns" this 
   * transfer request, which is typically an AgentMove
   * object's UID.
   */
  UID getOwnerUID();

  /**
   * Get the node-agent that contains the mobile agent
   * and is now transfering one of its agents.
   */
  MessageAddress getSource();

  /**
   * Get the node-agent that should add the mobile
   * agent and inform the source of the move status.
   */
  MessageAddress getTarget();

  /**
   * Get the mobility ticket, where
   * <ul>
   *   <li>This object's publisher is the ticket's 
   *       origin node</li>
   *   <li>This object's intended recipient is the 
   *       ticket's destination node</li>
   *   <li>The ticket specifies the mobile agent</li>
   * </ul>.
   * <p>
   * The addresses specified in the ticket are not 
   * null.
   */
  AbstractTicket getTicket();

  /**
   * Get the agent state.
   * <p>
   * <b>Note:</b> This state is <i>not</i> persisted!
   */
  StateTuple getState();

  /**
   * Get the current transfer status, which is one of 
   * the above "*_STATUS" constants.
   */
  int getStatusCode();

  /**
   * If (getStatusCode() == FAILED_STATUS), this is
   * the failure exception.
   */
  Throwable getFailureStackTrace();

  /**
   * For infrastructure use only!  Set the status, clear the
   * state.
   */
  void setStatus(int statusCode, Throwable stack);

}
