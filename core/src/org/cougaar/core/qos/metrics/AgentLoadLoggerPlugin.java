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

package org.cougaar.core.qos.metrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TimerTask;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.TopologyEntry;
import org.cougaar.core.service.TopologyReaderService;

public class AgentLoadLoggerPlugin 
    extends ComponentPlugin
    implements Constants
{
    private TopologyReaderService topologyService;
    private MetricsService metricsService;
    private String node;
    private String filename = "agent-data.log";
    private PrintWriter out;
    private DecimalFormat formatter = new DecimalFormat("00.00");
    private boolean first_time = true;
    private ArrayList agents;
    private long start;

    private class Poller extends TimerTask {
	public void run() {
	    if (first_time) 
		collectNames();
	    else
		dump();
	    first_time = false;
	}
    }

    public AgentLoadLoggerPlugin() {
	super();
    }


    private void collectNames() {
	start = System.currentTimeMillis();
	Set matches = null;
	try {
	    matches = topologyService.getAllEntries(null,  // Agent
						    node,
						    null, // Host
						    null, // Site
						    null); // Enclave
	} catch (Exception ex) {
	    // Node hasn't finished initializing yet
	    return;
	}
	if (matches == null) return;
	
	agents = new ArrayList();
	out.print("Time");
	Iterator itr = matches.iterator();
	while (itr.hasNext()) {
	    TopologyEntry entry = (TopologyEntry) itr.next();
	    if ((entry.getType() & TopologyReaderService.AGENT) == 0) continue;
	    String name = entry.getAgent();
	    out.print('\t');
	    out.print(name);
	    agents.add(name);
	}
	out.println("");
	out.flush();
    }

    private void dumpAgentData(String name) {
	String path  ="Agent(" +name+ ")" +PATH_SEPR+ CPU_LOAD_AVG_1_SEC_AVG;	
	Metric metric = metricsService.getValue(path);
	double value = metric.doubleValue();
	String formattedValue = formatter.format(value);
	out.print('\t');
	out.print(formattedValue);
    }

    private void dump() {
	out.print(MetricsServiceProvider.relativeTimeMillis()/1000.0);
	Iterator itr = agents.iterator();
	while (itr.hasNext()) {
	    String name = (String) itr.next();
	    dumpAgentData(name);
	}
	out.println("");
	out.flush();
    }

    public void load() {
	super.load();

	ServiceBroker sb = getServiceBroker();

	topologyService = (TopologyReaderService)
	    sb.getService(this, TopologyReaderService.class, null);

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);

	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
 	node = nis.getMessageAddress().toString();


	ThreadService threadService = (ThreadService)
	    sb.getService(this, ThreadService.class, null);

	filename = node+"-"+filename;
	
	try {
	    FileWriter fw = new FileWriter(filename);
	    out = new PrintWriter(fw);
	} catch (java.io.IOException ex) {
	    ex.printStackTrace();
	    return;
	}


	threadService.schedule(new Poller(), 60000, 500);
	
    }

    protected void setupSubscriptions() {
    }
  
    protected void execute() {
	//System.out.println("Uninteresting");
    }

}
