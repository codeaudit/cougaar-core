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

package org.cougaar.core.node;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import java.util.Set;

/** Service offered to components of NodeAgent to allow sufficient 
 * non-local access to the Node for control purposes.
 **/
public interface NodeBusyService extends Service {
  /**
   * The component using this must supply AgentIdentificationService
   * capable of correctly identifying the agent using the service
   * before it can call the setAgentBusy method.
   **/
  void setAgentIdentificationService(AgentIdentificationService ais);
  /**
   * The agent using this service must have already identified itself
   * using the setAgentIdentificationService before calling this
   * method. This avoids the possibilility of misrepresenting the
   * agent.
   **/
  void setAgentBusy(boolean busy);
  /**
   * Anybody can check if an agent is busy.
   **/
  boolean isAgentBusy(MessageAddress agent);
  /**
   * @return an unmodifiable set of busy agents (MessageAddress elements).
   **/
  Set getBusyAgents();
}
