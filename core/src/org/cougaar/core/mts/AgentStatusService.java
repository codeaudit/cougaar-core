/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.mts;

import java.util.Set;

import org.cougaar.core.component.Service;

public interface AgentStatusService extends Service
{
    int UNKNOWN = 0;
    int UNREGISTERED = 1;
    int UNREACHABLE = 2;
    int ACTIVE = 3;

    class AgentState {
	public long timestamp;
	public int status;
	public int queueLength;
	public int receivedCount;
	public long receivedBytes;
	public int lastReceivedBytes;
	public int sendCount;
      	public int deliveredCount;
	public long deliveredBytes;
	public int lastDeliveredBytes;
	public long deliveredLatencySum;
	public int lastDeliveredLatency;
	public double averageDeliveredLatency;
	public int unregisteredNameCount;
	public int nameLookupFailureCount;
	public int commFailureCount;
	public int misdeliveredMessageCount;
	public String lastLinkProtocolTried;
	public String lastLinkProtocolSuccess;
    }

    AgentState getRemoteAgentState(MessageAddress address);
    AgentState getLocalAgentState(MessageAddress address);

    Set getLocalAgents();
    Set getRemoteAgents();

    /**
     * @deprecated Use {@link #getRemoteAgentState}
     */
    AgentState getAgentState(MessageAddress address);

}

