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
import org.cougaar.core.society.MessageAddress;

import java.util.Observable;

public interface ResourceMonitorService extends Service
{
    double getExpectedMaxMJipsForAgent(MessageAddress agentAddress);
    //Note that this is in Jips and not MJips
    Observable getExpectedMaxJipsForAgentObservable(MessageAddress agentAddress);
    Object getExpectedMaxMJipsForAgentSyscond(MessageAddress agentAddress);


    double getExpectedEffectiveMJipsForAgent(MessageAddress agentAddress);
    Observable getExpectedEffectiveMJipsForAgentObservable(MessageAddress agentAddress);
    Object getExpectedEffectiveMJipsForAgentSyscond(MessageAddress agentAddress);


    double getExpectedBandwidthForAgent(MessageAddress agentAddress);
    Observable getExpectedBandwidthForAgentObservable(MessageAddress agentAddress);
    Object getExpectedBandwidthForAgentSyscond(MessageAddress agentAddress);



    double getExpectedCapacityForAgent(MessageAddress agentAddress);
    Observable getExpectedCapacityForAgentObservable(MessageAddress agentAddress);
    Object getExpectedCapacityForAgentSyscond(MessageAddress agentAddress);




    //double getExpectedMaxBandwidthToAgent(MessageAddress agentAddress);

   //  double getExpectedAvailableBandwidthToAgent(MessageAddress agentAddress);

//     double getExpectedMaxJipsForAgent(MessageAddress agentAddress);
//     double getExpectedAvailableJipsForAgent(MessageAddress agentAddress);

    String getHostForAgent(MessageAddress agentAddress);
    String getNodeForAgent(MessageAddress agentAddress);

}

