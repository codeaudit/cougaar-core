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
import org.cougaar.core.mts.MessageAddress;
import java.util.Observable;
import java.util.Observer;

/**
 * Basic Metric Service Client subscribes to a Metric path given in
 * the Agent's .ini file and prints the value if it ever changes
 */
public class MetricsClientPlugin 
    extends ParameterizedPlugin
    implements Constants
{
    protected MetricsService metricsService;
    protected MessageAddress agentID;
    private String paramPath = null;
    private VariableEvaluator evaluator;

  /**
   * Metric CallBack object
   */
    private class MetricsCallback implements Observer {
      /**
       * Call back implementation for Observer
       */

      public void update(Observable obs, Object arg) {
	    if (arg instanceof Metric) {
		Metric metric = (Metric) arg;
		double value = metric.doubleValue();
		System.out.println("Metric "+ paramPath +"=" + metric);
	    }
	}
    }
  
  /**
   * load time is when services are  lookup
   */ 
  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    
    // agentID = getAgentIdentifier();
    
    evaluator = new StandardVariableEvaluator(sb);

    metricsService = ( MetricsService)
      sb.getService(this, MetricsService.class, null);
	
    MetricsCallback cb = new MetricsCallback();
    paramPath = getParameter("path");
    if (paramPath == null) 
	paramPath ="$(localagent)"+PATH_SEPR+"LoadAverage";
    
    Object subscriptionKey=metricsService.subscribeToValue(paramPath, cb,
							   evaluator);

  }

    protected void setupSubscriptions() {
    }
  
    public synchronized void execute() {
    }
  
}






