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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cougaar.core.plugin.ComponentPlugin;

/**
 ** This Plugin is used to convert ordered standard parameter list from plugin
 * to key value pair paramteters
 */

public abstract class ParameterizedPlugin extends ComponentPlugin 
{
    private Map parameters = Collections.EMPTY_MAP;

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
     * @see #getParameterValues same as last value of getParameterValues list
     */ 
    protected String getParameter(String key, String default_val) {
        List l = (List) parameters.get(key);
        return (l == null ? default_val : (String) l.get(l.size()-1));
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

    /**
     * Get all parameters with the given key.
     * <p>
     * Input XML should look like:<pre>
     *   &lt;argument&gt;foo=alpha&lt;/argument&gt;
     *   &lt;argument&gt;foo=beta&lt;/argument&gt;
     * </pre>
     * The above example will be parsed as ["alpha", "beta"].
     *
     * @param key the string to be looked in argument list
     * @return either null or an unmodifiable, non-empty List of Strings
     */
    //List<String>
    public List getParameterValues(String key) {
        return (List) parameters.get(key);
    }

    /**
     * Get all parameters.
     * @return an unmodifiable Map of Strings to unmodifiable Lists of Strings
     */
    //Map<String,List<String>>
    public Map getParameterMap() {
        return parameters;
    }

    /** Called by plugin loader */
    public void setParameter(Object param) {
	if (!(param instanceof List)) return;

        // parse
        Map m = new HashMap();
        List a = (List) param;
        for (int i = 0, n = a.size(); i < n; i++) {
            Object o = a.get(i);
            if (!(o instanceof String)) continue;
            String s = (String) o;
            int sepr = s.indexOf('=');
            if (sepr < 0) continue;
            String key = s.substring(0, sepr);
            String value = s.substring(sepr+1);
            List l = (List) m.get(key);
            if (l == null) {
                l = new ArrayList();
                m.put(key, l);
            }
            l.add(value);
        }

        // make unmodifiable
        for (Iterator iter = m.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry me = (Map.Entry) iter.next();
            List l = (List) me.getValue();
            l = (l.size() == 1 ? 
                    Collections.singletonList(l.get(0)) :
                    Collections.unmodifiableList(l));
            me.setValue(l);
        }
        m = Collections.unmodifiableMap(m);

        // save
        this.parameters = m;
    }
}
