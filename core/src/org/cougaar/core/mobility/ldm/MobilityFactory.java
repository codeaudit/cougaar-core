/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

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
import org.cougaar.core.domain.Factory;
import org.cougaar.core.mobility.AbstractTicket;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;

/**
 * Factory to create MoveAgent objects.
 */
public interface MobilityFactory extends Factory {

  /**
   * Get a new ticket identifier for use when creating
   * a Ticket.
   */
  Object createTicketIdentifier();

  /**
   * Create a request that an agent be moved, which is
   * sent to the mobile agent itself.
   * <p>
   * This is the <i>typical usage</i>, where the request 
   * first passes through the mobile agent before passing 
   * to that agent's node.
   * <p>
   * Equivalent to calling:<pre>
   *   MessageAddress addr = ticket.getMobileAgent();
   *   if (addr == null) use the local agent
   *   createAgentMove(ownerUID, addr, ticket);
   * </pre>
   */
  AgentMove createAgentMove(
      UID ownerUID,
      MoveTicket ticket);

  /**
   * Create a request that an agent be moved, which
   * is sent directly to the specified target.
   * <p>
   * The target is either null, the mobile agent,
   * or the node running the mobile agent.  If the
   * mobile agent is specified, the request first
   * passes through the mobile agent.
   * <p>
   * In the future this may be used to send the request
   * directly to the node that contains the agent.
   */
  AgentMove createAgentMove(
      UID ownerUID,
      MessageAddress target,
      MoveTicket ticket);


  // old API


  /**
   * Create an agent move request -- this has been
   * replaced by the "AgentMove" APIs, but is still
   * supported.
   * 
   * @param ticket must have a ticket identifier that
   *    was created by the "createTicketIdentifier()"
   *    method.
   *
   * @see AgentMove new API
   */
  MoveAgent createMoveAgent(MoveTicket ticket);


  // the rest is for the infrastructure:


  /**
   * Create a inter-node agent transfer request, for
   * infrastructure use.
   */
  AgentTransfer createAgentTransfer(
      UID ownerUID,
      AbstractTicket ticket,
      StateTuple state);

}
