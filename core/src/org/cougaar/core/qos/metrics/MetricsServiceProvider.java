/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.NodeIdentifier;

public final class MetricsServiceProvider implements ServiceProvider
{
    
    private static final String RETRIEVER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.RSSMetricsServiceImpl";

    private static final String UPDATER_IMPL_CLASS =
	"org.cougaar.core.qos.rss.STECMetricsUpdateServiceImpl";


    private MetricsService retriever;
    private MetricsUpdateService updater;

    public MetricsServiceProvider(ServiceBroker sb, NodeIdentifier id) {
	Class[] parameters = { ServiceBroker.class, NodeIdentifier.class };
	Object[] args = { sb, id };
	try {
	    Class cl = Class.forName(RETRIEVER_IMPL_CLASS);
	    java.lang.reflect.Constructor cons = cl.getConstructor(parameters);
	    retriever = (MetricsService) cons.newInstance(args);
	} catch (ClassNotFoundException cnf) {
	    // qos jar not loaded
	} catch (Exception ex) {
	    ex.printStackTrace();
	}


	try {
	    Class cl = Class.forName(UPDATER_IMPL_CLASS);
	    java.lang.reflect.Constructor cons = cl.getConstructor(parameters);
	    updater = (MetricsUpdateService) cons.newInstance(args);
	} catch (ClassNotFoundException cnf) {
	    // qos jar not loaded
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == MetricsService.class) {
	    return retriever;
	} else if (serviceClass == MetricsUpdateService.class) {
	    return updater;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }


}

