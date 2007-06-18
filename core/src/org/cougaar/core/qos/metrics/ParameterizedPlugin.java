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

import java.util.List;
import java.util.Map;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.util.Arguments;

/**
 ** This Plugin is used to convert ordered standard parameter list from plugin
 * to key value pair paramteters
 */

public abstract class ParameterizedPlugin extends ComponentPlugin 
{
    protected Arguments args;

    public void setArguments(Arguments args) {
      this.args = args;
      try {
	    args.getAnnotations().setAllFields(this);
	} catch (Exception e) {
	    // TODO: Add Logging support when it's ready
	    throw new RuntimeException("Exception during field initialization", e);
	}
    }

    /** @see Arguments#getString(String) */
    protected String getParameter(String key) {
	return args.getString(key);
    }
    /** @see Arguments#getString(String,String) */
    protected String getParameter(String key, String defaultValue) {
        return args.getString(key, defaultValue);
    }
    /** @see Arguments#getLong(String,long) */
    public long getParameter(String key, long defaultValue) {
        return args.getLong(key, defaultValue);
    }
    /** @see Arguments#getDouble(String,double) */
    public double getParameter(String key, double defaultValue) {
        return args.getDouble(key, defaultValue);
    }
    /** @see Arguments#getStrings(String) */
    public List getParameterValues(String key) {
        return args.getStrings(key);
    }
    /** @see Arguments */
    public Map getParameterMap() {
        return args;
    }
}
