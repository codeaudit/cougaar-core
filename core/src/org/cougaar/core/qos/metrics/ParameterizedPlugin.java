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

// TO BE DONE move to cougaar utilities package
package org.cougaar.core.qos.metrics;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.cougaar.core.plugin.ComponentPlugin;

/**
 ** This Plugin is used to convert ordered standard parameter list from plugin
 * to key value pair paramteters
 */

public abstract class ParameterizedPlugin extends ComponentPlugin 
{
    private Properties parameters= new Properties();

    /**
     * Given a key, looks up parameter arg in the plugin's comma
     * delimited argument list specified in *Agent.ini file and
     * returns its values
     * @param key the string to be looked in argument list
     * @return String value of key
     */
    protected String getParameter(String key) {
	return getParameter(key, null);
    }

    /**
     *  Given a key, looks up parameter arg in the plugin's comma
     * delimited argument list specified in *Agent.ini file and
     * returns its value only if it is not null else return the
     * default value
     * @param key the string to be looked in argument list
     * @param default_val the default value
     * @return String value of key if it is not null else the default value
     */ 
    protected String getParameter(String key, String default_val) {
	String value = parameters.getProperty(key);
        return (value == null ? default_val : value);
    }

    public long getParameter(String key, long defaultValue) {
	String spec = getParameter(key);
	if (spec != null) {
	    try { return Long.parseLong(spec); }
	    catch (NumberFormatException ex) { return defaultValue; }
	} else {
	    return defaultValue;
	}
    }

    public double getParameter(String key, double defaultValue) {
	String spec = getParameter(key);
	if (spec != null) {
	    try { return Double.parseDouble(spec); }
	    catch (NumberFormatException ex) { return defaultValue; }
	} else {
	    return defaultValue;
	}
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
		parameters.setProperty(key, value);
	    }
	}
    }



}
