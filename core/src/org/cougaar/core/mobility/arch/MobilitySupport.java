/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

package org.cougaar.core.mobility.arch;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Required support to the mobility handlers.
 */
public interface MobilitySupport {

  // constants for the handler invocation

  LoggingService getLog();

  /** id of the agent. */
  MessageAddress getId();

  /** id of this node. */
  MessageAddress getNodeId();

  MoveTicket getTicket();

  // message-sender

  void sendTransfer(ComponentDescription desc, Object state);

  void sendAck();

  void sendNack(Throwable throwable);

  // agent-container

  /** add an agent with the above "getId()" and given state. */
  void addAgent(ComponentDescription desc);

  /** remove the agent with the above "getId()". */
  void removeAgent();

  // removable mobility-listener

  void onDispatch();

  void onArrival();

  void onFailure(Throwable throwable);

  void onRemoval();

}
