/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.mobility;

import org.cougaar.core.mts.MessageAddress;

/**
 * Callback interface for the requestor of the 
 * MobilityListenerService, which allows a component
 * to observe mobility events for <i>its</i> agent.
 * <p>
 * Clients should consider using the "MoveAgentPlugin" instead
 * of directly using the MobilityService.  Mobility listen
 * callbacks can occur outside of plugin blackboard transactions,
 * so special care is needed.
 *
 * @see BufferedMobilityListener
 */
public interface MobilityListener {

  /** Address of <i>this</i> agent. */
  MessageAddress getAddress();

  /** about to move. */
  void onDispatch(Ticket ticket);

  /** move succeeded. */
  void onArrival(Ticket ticket);

  /** move failed, restart at original location. */
  void onFailure(Ticket ticket, Throwable t);

}
