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

package org.cougaar.core.qos.metrics;

import java.util.Observable;
import java.util.Observer;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;

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
    metricsService.subscribeToValue(paramPath, cb, evaluator);
  }

    protected void setupSubscriptions() {
    }
  
    public synchronized void execute() {
    }
  
}






