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

package org.cougaar.core.thread;

import java.util.Timer;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;

/**
 * This class creates a servlet which displays the state of COUGAAR
 * ThreadService threads (Schedulables) in a way that's vaguely
 * remniscent of the unix 'top' command.
 * 
 * This is designed to be a Node-level plugin.
 */
public class TopPlugin extends ParameterizedComponent // not a Plugin
{
    private static final long DEFAULT_SAMPLE_PERIOD = 5000;
    private static final String WARN_TIME_VALUE="100";
    private static final String WARN_TIME_PARAM= 
	"warn-time";
    private static final String INFO_TIME_VALUE="10";
    private static final String INFO_TIME_PARAM= 
	"info-time";

    private RogueThreadDetector rtd;
    private ServiceBroker sb;

    public TopPlugin() {
	super();
    }



    public void load() {
	super.load();

	String run_servlet = getParameter("servlet", "true");
	String run_timer = getParameter("detector", "true");
	long sample_period = getParameter("period", DEFAULT_SAMPLE_PERIOD);
	String test = getParameter("test", "false");
	ServiceBroker sb = getServiceBroker();
	if (run_servlet.equalsIgnoreCase("true")) {
	    new TopServlet(sb);
	}
	if (run_timer.equalsIgnoreCase("true")) {
	    rtd = new RogueThreadDetector(sb, sample_period);
	    
	    // initialize param subscriptions
// 	    initializeParameter(WARN_TIME_PARAM,WARN_TIME_VALUE);
// 	    initializeParameter(INFO_TIME_PARAM,INFO_TIME_VALUE);
	    dynamicParameterChanged(WARN_TIME_PARAM,
				    getParameter(WARN_TIME_PARAM,
						 WARN_TIME_VALUE));
	    dynamicParameterChanged(INFO_TIME_PARAM,
				    getParameter(INFO_TIME_PARAM,
						 INFO_TIME_VALUE));

	    // We can't use the ThreadService for the poller because it
	    // may run out of pooled threads, which is what we are trying
	    // to detect. 
	    Timer timer = new Timer();
	    timer.schedule(rtd, 0, sample_period);
	}
	if(test.equalsIgnoreCase("true")) {
	    runTest();
	}
    }

    protected void dynamicParameterChanged(String name, String value)
    {
	if (name.equals(WARN_TIME_PARAM)) {
	    int warnTime = Integer.parseInt(value) *1000; //millisecond
	    rtd.setWarnTime(warnTime);
	} else if (name.equals(INFO_TIME_PARAM)) {
	    int infoTime = Integer.parseInt(value) *1000; //millisecond
	    rtd.setInfoTime(infoTime);
	}
    }

    void runTest() {
	Runnable test = new Runnable() {
		public void run () {
		    int type = SchedulableStatus.OTHER;
		    String excuse = "Calls to sleep() are evil";
		    SchedulableStatus.beginBlocking(type, excuse);
		    try { Thread.sleep(100000); }
		    catch (InterruptedException ex) {}
		    SchedulableStatus.endBlocking();
		}
	    };
	ServiceBroker sb = getServiceBroker();
	ThreadService tsvc = (ThreadService) sb.getService(this, ThreadService.class, null);
	org.cougaar.core.thread.Schedulable sched = tsvc.getThread(this, test, "Sleep test");
	sb.releaseService(this, ThreadService.class, tsvc);
	sched.schedule(0, 10);
    }

    public final void setBindingSite(BindingSite bs) {
	sb = bs.getServiceBroker();
    }


    public ServiceBroker getServiceBroker() {
	return sb;
    }

}
