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

package org.cougaar.core.node;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.SuicideService;
import org.cougaar.core.thread.ThreadStatusService;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

public class StateDumpServiceComponent
    extends ParameterizedComponent
    implements ServiceProvider
{
    private static Logger logger = Logging.getLogger(StateDumpServiceComponent.class);
    private ServiceBroker sb, rootsb;
    private StateDumpService impl;

    public StateDumpServiceComponent()
    {
    }


    public void setNodeControlService(NodeControlService ncs) { 
	rootsb = (ncs == null)?null:ncs.getRootServiceBroker();
    }

    public void load()
    {
	super.load();
	impl = new Impl(sb);
	rootsb.addService(StateDumpService.class, this);
    }

    public void start()
    {
	super.start();
	int dieTime = (int) getParameter("dieTime", 0);
	if (dieTime > 0) {
	    java.util.Timer timer = new java.util.Timer();
	    java.util.TimerTask task = new java.util.TimerTask() {
		    public void run() {
			SuicideService svc = (SuicideService)
			    sb.getService(this, SuicideService.class, null);
			Throwable thr = new Error("Pointless suicide");
			svc.die(StateDumpServiceComponent.this, thr);
		    }
		};
	    timer.schedule(task, dieTime);
	}
    }

    public Object getService(ServiceBroker sb, 
			     Object requestor,
			     Class serviceClass)
    {
	if (serviceClass == StateDumpService.class)
	    return impl;
	else
	    return null;
    }


    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


    public final void setBindingSite(BindingSite bs) {
	this.sb = bs.getServiceBroker();
    }

    private static class Impl
	implements StateDumpService
    {
	ThreadStatusService threadStatus;

	Impl(ServiceBroker sb)
	{
	    threadStatus = (ThreadStatusService)
		sb.getService(this, ThreadStatusService.class, null);
	}

	private void dumpThreads()
	{
	    try {
		FileInputStream fis = new FileInputStream("/proc/self/status");
		Properties props = new Properties();
		props.load(fis);
		String pid = props.getProperty("Pid");
		String command = "kill -3 " + pid;
		Runtime.getRuntime().exec(command);
	    } catch (Exception ex) {
		logger.error(null, ex);
	    }
	}

	private void dumpSchedulables()
	{
	    List status = threadStatus.getStatus();
	    if (status != null) {
		int size = status.size();
		logger.warn(size + " Schedulables");
		for (int i=0; i<size; i++) {
		    ThreadStatusService.Record record =
			(ThreadStatusService.Record) status.get(i);
		    boolean queued = record instanceof ThreadStatusService.QueuedRecord;
		    logger.warn("Schedulable " +i+
				" " + (queued ? "Queued" : "Running") +
				" " + record.elapsed +
				" " + record.scheduler +
				" " + record.lane +
				" " + record.schedulable+
				" " + record.consumer
				);
		}
	    }
	}

	public void dumpState()
	{
	    dumpSchedulables();
	    dumpThreads();
	}
    }

}

