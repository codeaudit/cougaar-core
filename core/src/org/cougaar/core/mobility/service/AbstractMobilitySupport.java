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

package org.cougaar.core.mobility.service;

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.mobility.MoveTicket;
import org.cougaar.core.mobility.arch.MobilitySupport;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;

/**
 * Base class for mobility-support class.
 */
abstract class AbstractMobilitySupport
implements MobilitySupport 
{

  protected final MessageAddress id;
  protected final MessageAddress nodeId;
  protected final MoveTicket moveTicket;
  protected final LoggingService log;

  public AbstractMobilitySupport(
      MessageAddress id,
      MessageAddress nodeId,
      MoveTicket moveTicket,
      LoggingService log) {
    this.id = id;
    this.nodeId = nodeId;
    this.moveTicket = moveTicket;
    this.log = log;
  }

  // fields

  public LoggingService getLog() {
    return log;
  }

  public MessageAddress getId() {
    return id;
  }

  public MessageAddress getNodeId() {
    return nodeId;
  }

  public MoveTicket getTicket() {
    return moveTicket;
  }

  // model-reg

  // agent-container

  public abstract void addAgent(ComponentDescription desc);

  public abstract void removeAgent();

  public String toString() {
    return "Mobility support for agent "+id+" on "+nodeId;
  }

}
