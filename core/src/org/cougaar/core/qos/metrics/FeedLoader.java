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

import org.cougaar.core.component.ServiceBroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class FeedLoader extends QosComponent
{
    private static final Class[] ParamTypes =  { String[].class };
    private String[] args;
    private String name;
    private String classname;

    private String[] parseArgs(String raw) {
	ArrayList temp = new ArrayList();
	StringTokenizer tokenizer = new StringTokenizer(raw, " ");
	while (tokenizer.hasMoreTokens()) {
	    String token = tokenizer.nextToken().trim();
	    temp.add(token);
	}
	//JAZ Object[] can not be cast to Sting[], 
	// Must be a better way
	String[] returnValue = new String[temp.size()];
	int i;
	for (i = 0; i< temp.size(); i++) returnValue[i]=(String) temp.get(i);
	return returnValue;
    }

    private String getURL() {
	int i ;
	for (i = 0; i < (args.length -1); i++) {
	    if ( args[i].equals("-url") )
		return args[i+1];
	}
	return null;
    }

    public void setParameter(Object param) {
	if (param instanceof List) {
	    Iterator itr = ((List) param).iterator();
	    while(itr.hasNext()) {
		String property = (String) itr.next();
		int sepr = property.indexOf('=');
		if (sepr < 0) continue;
		String key = property.substring(0, sepr);
		String value = property.substring(++sepr);
		if (key.equalsIgnoreCase("name"))
		    name = value;
		else if (key.equalsIgnoreCase("class"))
		    classname = value;
		else if (key.equalsIgnoreCase("args"))
		    args=parseArgs(value);
	    }
	}
    }

    public void start() {
	ServiceBroker sb = getServiceBroker();
	DataFeedRegistrationService svc = (DataFeedRegistrationService)
	    sb.getService(this, DataFeedRegistrationService.class, null);
	if (svc != null && name != null && 
	    classname != null  && args != null ) {
	    Object feed = null;
	    try {
		Class cl = Class.forName(classname);
		java.lang.reflect.Constructor constructor = 
		    cl.getConstructor(ParamTypes);
		Object[] argList = { args };
		feed = constructor.newInstance(argList);
		svc.registerFeed(feed, name);
		if (name.equalsIgnoreCase("sites")){
		    // special Feed that has the URL for the sites file
		    svc.populateSites(getURL());
		}
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }

	}
    }
}
