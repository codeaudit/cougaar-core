/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.qos.monitor;

import org.cougaar.core.component.Service;
import org.cougaar.core.society.MessageAddress;

import java.util.Observable;

public class ResourceMonitorServiceImpl implements ResourceMonitorService
{
    
    public double getJipsForAgent(MessageAddress agentAddress) {
	return 10.0;
    }

    public Observable getJipsForAgentObservable(MessageAddress agentAddress) {
	return null;
    }

}

