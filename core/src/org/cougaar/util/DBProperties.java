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

/*
 * Originally from delta/fgi package mil.darpa.log.alpine.delta.plugin;
 * Copyright 1997 BBN Systems and Technologies, A Division of BBN Corporation
 * 10 Moulton Street, Cambridge, MA 02138 (617) 873-3000
 */

package org.cougaar.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * This utility extends java.util.properties by doing parameter
 * substitutions after loading a .q file. Don't know why there are so
 * many variants of this code scattered around. This handles all the
 * cases I am aware of.
 **/
public class DBProperties extends java.util.Properties {
    private String dbtype;
    private String dbspec;
    private boolean debug = false;

    /**
     * Read and parse a .q file relative to a particular database. The
     * database is specified in terms of a key in cougaar.rc, for
     * example, "org.cougaar.database".
     **/
    public static DBProperties readQueryFile(String dbspec, String qfile)
    throws IOException
    {
        InputStream i = new BufferedInputStream(ConfigFinder.getInstance().open(qfile));
        try {
            DBProperties result = new DBProperties(dbspec);
            result.load(i);
            return result;
        } finally {
            i.close();
        }
    }

    /**
     * Constructor for a particular database specification. The
     * database specification is the name of a database url parameter
     * found using the Parameters class. This is typically in
     * cougaar.rc.
     **/
    private DBProperties(String dbspec) {
        this.dbspec = dbspec;   // For debugging
        String dburl = Parameters.findParameter(dbspec);
        int ix1 = dburl.indexOf("jdbc:") + 5;
        int ix2 = dburl.indexOf(":", ix1);
        dbtype = dburl.substring(ix1, ix2);
    }

    /**
     * Accessor the the database type string
     **/
    public String getDBType() {
        return dbtype;
    }

    /**
     * Load properties from an InputStream and post-process to perform
     * variable substitutions on the values. Substitution is done
     * using Parameters.replaceParameters.
     **/
    public void load(InputStream i) throws IOException {
	super.load(i);
	for (Enumeration enum = propertyNames(); enum.hasMoreElements(); ) {
	    String name = (String) enum.nextElement();
	    String rawValue = getProperty(name);
	    String cookedValue = Parameters.replaceParameters(rawValue, this);
            setProperty(name, cookedValue);
        }
    }

    /**
     * Return a query with a given name. Variable substitution is
     * performed by looking for patterns which are the keys in a Map.
     * By convention, variable names start with a colon and are
     * alphanumeric, but this is not required. However, variable names
     * must not start with the name of another variable. For example,
     * :a and :aye are not allowed. The query is sought under two
     * different names, first by suffixing the given name with a dot
     * (.) and the database type and, if that query is not found,
     * again without the suffix.
     * @param queryName the name of the query.
     * @param substitutions The substitutions to be performed. The
     * query is examined for occurances of the keys in the Map and
     * replaced with the corresponding value.
     **/
    public String getQuery(String queryName, Map substitutions) {
        String result = getQuery1(queryName + "." + dbtype, substitutions);
        if (result == null) result = getQuery1(queryName, substitutions);
        return result;
    }

    /**
     * Does the actual work. Called twice for each query.
     **/
    private String getQuery1(String queryName, Map substitutions) {
        String query = getProperty(queryName);
        if (query != null && substitutions != null &&
            !substitutions.isEmpty()) {
            for (Iterator keys = substitutions.keySet().iterator(); keys.hasNext(); ) {
                String key = (String) keys.next();
                int ix = 0;
                while ((ix = query.indexOf(key, 0)) >= 0) {
                    String subst = (String) substitutions.get(key);
                    query = query.substring(0, ix)
                        + subst
                        + query.substring(ix + key.length());
                    ix += subst.length();
                }
            }
        }
        if (query != null && debug) {
            System.out.println(this + ": " + queryName + "->" + query);
        }
        return query;
    }

    /**
     * Enable debugging. When debugging is enabled, the queries are
     * printed after substitution has been performed
     **/
    public void setDebug(boolean newDebug) {
        debug = newDebug;
    }

    public String toString() {
        return dbspec;
    }
}
