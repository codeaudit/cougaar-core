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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;

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

    public void load() {
        super.load();
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
		LoggingService lsvc = (LoggingService)
		    sb.getService(this, LoggingService.class, null);
		if (lsvc.isErrorEnabled())
		    lsvc.error("Error creating DataFeed: " + ex);
	    }

	}
    }
}
