/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.mts.MessageAddress;
import java.util.Map;

public interface QuiescenceReportService extends Service {
  /**
   * Specifies the client agent's identity using his
   * AgentIdentificationService. This method must be called before any
   * other method and establishes the identity of the client agent in
   * a more or less unforgable way.
   * @param agentIdentificationService the agent's identification service
   **/
  void setAgentIdentificationService(AgentIdentificationService agentIdentificationService);

  /**
   * Specify the maps of quiescence-relevant outgoing and incoming
   * message numbers associated with a newly achieved state of
   * quiescence. For efficiency, this method should be called before
   * calling setQuiescentState().
   * @param outgoingMessageNumbers a Map from agent MessageAddresses
   * to Integers giving the number of the last message sent. The
   * arguments must not be null.
   * @param incomingMessageNumbers a Map from agent MessageAddresses
   * to Integers giving the number of the last message received. The
   * arguments must not be null.
   **/
  void setMessageNumbers(Map outgoingMessageNumbers, Map incomingMessageNumbers);

  /**
   * Specifies that, from this service instance point-of-view, the
   * agent is quiescent. The message number maps are not altered.
   **/
  void setQuiescentState();

  /**
   * Specifies that this agent is no longer quiescent.
   **/
  void clearQuiescentState();
}
