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

public interface QosMonitorService extends Service
{
    public static final int NOT_CREATED = 0;
    public static final int ACTIVE = 1;
    public static final int MOVING = 2;
    public static final int RESTARTED = 3;
    public static final int MISSING = 4;
    public static final int UNKNOWN = 5;

    public int getAgentStatus(MessageAddress agentAddress);
}

