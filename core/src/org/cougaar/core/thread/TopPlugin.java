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

import java.util.Timer;

import org.cougaar.core.component.ParameterizedComponent;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.ThreadService;

/**
 * This component is used to create the {@link TopServlet}, which it
 * will do unless the "servlet" parameter is set to "false", and the
 * {@link RogueThreadDetector}, which it will do unless the "detector"
 * parameter is set to "false".  
 * 
 * The period of the @link RogueThreadDetector defaults to 5000 and
 * can be set to some other value with the "period" value.  It will
 * run as a TimerTask in a native @link Timer, rather than using
 * the {@link ThreadService}.
 *
 * As if that weren't enough, it can also be used for yet another
 * purpose, to run a test of the @link RogueThreadDetector, which it
 * exercises by running a Schedulable that intentionally runs for too
 * long.  To enable this test, set the "test" parameter to "true".
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

    public void setServiceBroker(ServiceBroker sb) {
        this.sb = sb;
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
	    long lane = getParameter("lane", ThreadService.BEST_EFFORT_LANE);
	    runTest((int) lane);
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

    void runTest(int lane) {
	Runnable test = new Runnable() {
		public void run () {
		    int type = SchedulableStatus.CPUINTENSIVE;
		    String excuse = "Calls to sleep() are evil";
		    SchedulableStatus.beginBlocking(type, excuse);
		    try { Thread.sleep(100000); }
		    catch (InterruptedException ex) {}
		    SchedulableStatus.endBlocking();
		}
	    };
	ServiceBroker sb = getServiceBroker();
	ThreadService tsvc = (ThreadService) 
	    sb.getService(this, ThreadService.class, null);
	org.cougaar.core.thread.Schedulable sched = 
	    tsvc.getThread(this, test, "Sleep test", lane);
	sb.releaseService(this, ThreadService.class, tsvc);
	sched.schedule(0, 10);
    }

    public ServiceBroker getServiceBroker() {
	return sb;
    }

}
