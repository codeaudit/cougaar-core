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

package org.cougaar.core.thread;

import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.TimerTask;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadControlService;


/**
 * This plugin periodically scans all the schedulable and prints out
 * error messages if they are taking a long time to complete. Error
 * messages are printed when a schedulable is holding a pooled thread
 * (in run state) at 10, 30, 100, 300 and 1000 seconds. 
 *
 *  This is designed to be a Node-level plugin.
 */
class RogueThreadDetector
    extends TimerTask
{
    private ThreadStatusService statusService;
    private ThreadControlService controlService;
    private LoggingService loggingService;
    private long samplePeriod;
    private long[] limits;
    private int warnTime;
    private int infoTime;

    RogueThreadDetector(ServiceBroker sb, long samplePeriod) {
	this.samplePeriod = samplePeriod;

	// Less ugly than writing a proper math function (really).
	long too_long = samplePeriod*2;
	limits = new long[7];
	limits[0] = too_long;
	limits[1] = too_long*3;
	limits[2] = too_long*10;
	limits[3] = too_long*30;
	limits[4] = too_long*100;
	limits[5] = too_long*300;
	limits[6] = too_long*1000;

	    


	NodeControlService ncs = (NodeControlService)
	    sb.getService(this, NodeControlService.class, null);

	if (ncs == null) {
	    throw new RuntimeException("Unable to obtain service");
	}

	ServiceBroker rootsb = ncs.getRootServiceBroker();

	loggingService = (LoggingService)
	    sb.getService(this, LoggingService.class, null);

	statusService = (ThreadStatusService)
	    rootsb.getService(this, ThreadStatusService.class, null);
	controlService = (ThreadControlService)
	    rootsb.getService(this, ThreadControlService.class, null);
	if (statusService == null) {
	    throw new RuntimeException("Unable to obtain service");
	}
    }

    void setWarnTime(int warnTime)
    {
	this.warnTime = warnTime;
    }

    void setInfoTime(int infoTime)
    {
	this.infoTime = infoTime;
    }


	
    private boolean timeToLog(long deltaT) {
	for (int i=0; i<limits.length; i++) {
	    long lowerBound = limits[i];
	    if (deltaT < lowerBound) return false;
	    long upperBound = lowerBound + samplePeriod;
	    if (deltaT < upperBound) return true;
	}
	return false;
    }


    private String warningMessage(ThreadStatusService.Record record) 
    {
	String b_string = 
	    SchedulableStatus.statusString(record.blocking_type,
					   record.blocking_excuse);
	return "Schedulable running for too long: Millisec=" +record.elapsed+
	    " Level=" +record.scheduler+
	    " Schedulable=" +record.schedulable+
	    " Client=" +record.consumer+
	    " Blocking=" +b_string;

    }

    private void detectRogue(List status) 
    {
	int running = 0;
	int queued = 0;
	
	boolean useItr = (status instanceof RandomAccess);
	Iterator itr = (useItr ? status.iterator() : null);
	for (int i = 0, n = status.size(); i < n; i++) {
	    ThreadStatusService.Record record = (ThreadStatusService.Record)
                (useItr ? itr.next() : status.get(i));
	    if (record.getState() == ThreadStatusService.RUNNING){
		running++;
		if (loggingService.isWarnEnabled() && 
		    timeToLog(record.elapsed)) {
		    if (record.elapsed >= warnTime) 
			loggingService.warn(warningMessage(record));
		    else if (loggingService.isInfoEnabled() &&
			     record.elapsed >= infoTime) 
			loggingService.info(warningMessage(record));
		}
	    } else {
		queued++;
	    }
	}
	if (controlService != null && loggingService.isInfoEnabled()) {
	    int max = controlService.maxRunningThreadCount();
	    if (running >= max || queued >= 1) {
		// running can be > max because the construction of
		// the status list isn't synchronized.
		loggingService.info("ThreadService is using all the pooled threads: running="
				    +running+ " queued=" +queued);
	    }
	}
    }

    public void run() 
    {
	List status = statusService.getStatus();
	if (status == null) {
	    // print some error message
	    loggingService.error("Thread Status Service returned null");
	    return;
	}

	if (status.size() > 0) 	detectRogue(status);
    }
}
