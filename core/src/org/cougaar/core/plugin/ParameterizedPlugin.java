/*
 *
 * Copyright 2007 by BBN Technologies Corporation
 *
 */

package org.cougaar.core.plugin;

import java.util.List;
import java.util.Map;

import org.cougaar.util.Arguments;

public abstract class ParameterizedPlugin extends ComponentPlugin {
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
    public List<String> getParameterValues(String key) {
        return args.getStrings(key);
    }

    /** @see Arguments */
    public Map<String, List<String>> getParameterMap() {
        return args;
    }

}
