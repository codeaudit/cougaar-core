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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.servlet.ServletFrameset;
import org.cougaar.core.service.wp.WhitePagesService;

public abstract class MetricsServlet 
    extends ServletFrameset
    implements Constants
{

    protected WhitePagesService wpService;
    protected MetricsService metricsService;
    protected final DecimalFormat f4_2 = new DecimalFormat("#0.00");
    protected final DecimalFormat f6_3 = new DecimalFormat("##0.000");
    protected final DecimalFormat f2_0 = new DecimalFormat("#0");
    protected final DecimalFormat f3_0 = new DecimalFormat("##0");
    protected final DecimalFormat f4_0 = new DecimalFormat("###0");
    protected final DecimalFormat f7_0 = new DecimalFormat("#######0");
    
    private AgentContainer agentContainer;

    public MetricsServlet(ServiceBroker sb) 
    {
	super(sb);

	wpService = (WhitePagesService)
	    sb.getService(this, WhitePagesService.class, null);

	NodeControlService ncs = (NodeControlService)
            sb.getService(this, NodeControlService.class, null);
        if (ncs != null) {
            agentContainer = ncs.getRootContainer();
            sb.releaseService(this, NodeControlService.class, ncs);
        }

	metricsService = (MetricsService)
	    sb.getService(this, MetricsService.class, null);


    }

    /**
     * @return the message addresses of the agents on this
     * node, or null if that information is not available.
     */
    protected final Set getLocalAgents() 
    {
        if (agentContainer == null) {
            return null;
        } else {
            return agentContainer.getAgentAddresses();
        }
    }


    public void printBottomPage(PrintWriter out) 
    {
	out.print("<p><b>Color key</b>");
	Color.colorTest(out);
    }

    public int dataPercentage() 
    {
	return 70;
    }

    public int bottomPercentage() 
    {
	return 20;
    }

}
