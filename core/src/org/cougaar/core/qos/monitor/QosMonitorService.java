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

package org.cougaar.core.qos.monitor;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;

/**
 * @deprecated topology-related queries are now handled by
 *    the TopologyReaderService, and MTS-related queries
 *    will be handled by future MTS-specific service (TBA)
 */
public interface QosMonitorService extends Service
{
    int UNKNOWN = 0;
    int NOT_CREATED = 1;
    int ACTIVE = 2;
    int MOVING = 3;
    int RESTARTED = 4;
    int MISSING = 5;
    int FAILING = 6;
    
    /**
     * Uses the naming service to determine the status of an Agent
     */
    int lookupAgentStatus(MessageAddress agentAddress);

    /**
     * Get the cached status, or look it up if the cache is stale. 
     **/
    int getAgentStatus(MessageAddress agentAddress);

    int getAgentCommStatus(MessageAddress agentAddress);

    String lookupHostForAgent(MessageAddress agentAddress);
    String lookupNodeForAgent(MessageAddress agentAddress);
    String getNodeForAgent(MessageAddress agentAddress);
    String getHostForAgent(MessageAddress agentAddress);

    String lookupHostForNode(String name);
    String getHostForNode(String name);

}

