/*
 * <copyright>
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.qos.metrics;


import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;

import java.io.PrintStream;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;


public class MetricsBlasterPlugin
    extends ParameterizedPlugin
    implements Observer
{
    
    private MetricsUpdateService update;
    private MetricsService svc;
    
    private String key,path;
	
    private long callbackCount =0;
    private long blastCount=0;
    private long lastCallbackDelay=0;
    private long lastPrintTime=0;

    private Timer timer = new Timer();
    private int restCount = 0;
    private TimerTask restTask;
	

    private void dumpCounters(long now) {
	if (1000 <  (now - lastPrintTime)){
	    System.out.println("blast count=" +blastCount+
			       " callback count=" +callbackCount+
			       " Last delay=" +lastCallbackDelay);
	    lastPrintTime=now;
	}
    }

    public void execute() {
    }

    public void setupSubscriptions() {
    }


    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();
	update = (MetricsUpdateService)
	    sb.getService(this, MetricsUpdateService.class, null);
	svc = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	path = getParameter("path");
	if (path != null) {
	    svc.subscribeToValue(path, this);
	    System.out.println("Subscribed to " +path);


	    key = getParameter("key");
	    if (key !=null) {
		System.out.println("Blasting to " +key);
		timer.schedule(new Blast(), 10000);
	    }

	}
    }


    public void update(Observable o, Object arg) {
	callbackCount++;
	long now = System.currentTimeMillis();
	long value = ((Metric) arg).longValue();
	lastCallbackDelay = now - value;
    }

    private class Blast extends TimerTask {
	public void run() {
	    System.out.println("Starting Blaster");
	    long startTime =  System.currentTimeMillis();
	    long startBlast = blastCount;
	    long startCallback = callbackCount;

	    long now = startTime;
	    // Blast for 5 seconds and then stop
	    while (5000 > (now-startTime)) {
		now =  System.currentTimeMillis();
		Metric m = new MetricImpl(new Long(now),
					  0.3,
					  "", "MetricsTestAspect");
		update.updateValue(key, m);
		blastCount++;
		dumpCounters(now);
	    }
	    float deltaBlasts = (blastCount-startBlast);
	    float deltaCallback = (callbackCount-startCallback);
	    long deltaTime = now - startTime;
	    float blastRate = deltaBlasts/deltaTime;
	    float callbackRate = deltaCallback/deltaTime;
	    float callbackPercent = deltaBlasts > 0 ? 
		(100*deltaCallback)/deltaBlasts :
		0.0f;
	    System.out.println("Stopped Blaster:" +
			       "blasts/millisec =" +  blastRate +
			       "callback/millisec =" +  callbackRate +
			       " Callback % =" + callbackPercent);

	    restCount = 0;
	    restTask = new Rest();
	    timer.schedule(restTask, 0, 1000);
	}
    }

    private class Rest extends TimerTask {
	public void run() {
	    dumpCounters(System.currentTimeMillis());
	    if (restCount++ == 10) {
		timer.schedule(new Blast(), 0);
		restTask.cancel();
	    }
	}
    }


}
