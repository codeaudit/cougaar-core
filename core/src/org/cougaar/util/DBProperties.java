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

import java.net.URL;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
    private String default_dbtype;
    private boolean debug = false;
    private String name;

    /**
     * Read and parse a .q file. If the .q file contains value for
     * "database" the default database type is set accordingly. The
     * default database type may be set or changed manually with the
     * setDefaultDatabase method.
     * @param qfile the name of the query file
     **/
    public static DBProperties readQueryFile(String qfile)
        throws IOException
    {
        return createDBProperties(qfile, ConfigFinder.getInstance().open(qfile));
    }

    /**
     * Read and parse a .q file specified as a URL. If the .q file
     * contains a value for "database" the default database type is
     * set accordingly. The default database type may be set or
     * changed manually with the setDefaultDatabase method.
     * @param url the url to be opened to read the query file
     * contents.
     **/
    public static DBProperties readQueryFile(URL url)
    throws IOException
    {
        return createDBProperties(url.toString(), url.openStream());
    }

    private static DBProperties createDBProperties(String name, InputStream is)
        throws IOException
    {
        InputStream i = new BufferedInputStream(is);
        try {
            DBProperties result = new DBProperties(name);
            result.load(i);
            String dburl = result.getProperty("database");
            if (dburl != null) result.setDefaultDatabase(dburl);
            return result;
        } finally {
            i.close();
        }
    }

    private DBProperties(String name) {
        this.name = name;
    }

    /**
     * Change the database specification. The database specification
     * is the name of a database url parameter found using in this
     * DBProperties.
     * @param dburl the jdbc url of the database in which the queries
     * are to be executed. The database type is extracted from the url
     * and used for getting queries tailored for that database.
     **/
    public void setDefaultDatabase(String dburl) {
        default_dbtype = getDBType(dburl);
    }

    /**
     * Accessor for the default database type string. This is the
     * string that is appended to query names (e.g. oracle) to form
     * the name of a database-specific query (or other value).
     **/
    public String getDBType() {
        return default_dbtype;
    }

    /**
     * Convert a db url into a database type string
     **/
    private String getDBType(String dburl) {
        int ix1 = dburl.indexOf("jdbc:") + 5;
        int ix2 = dburl.indexOf(":", ix1);
        return dburl.substring(ix1, ix2);
    }

    /**
     * Load properties from an InputStream and post-process to perform
     * variable substitutions on the values. Substitution is done
     * using Parameters.replaceParameters.
     * This method should not normally be used and is defined only to
     * override the base class version and interpose parameter
     * replacements from cougaar.rc.
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
     * (.) and the default database type and, if that query is not
     * found, again without the suffix.
     * @param queryName the name of the query.
     * @param substitutions The substitutions to be performed. The
     * query is examined for occurances of the keys in the Map and
     * replaced with the corresponding value.
     **/
    public String getQuery(String queryName, Map substitutions) {
        return getQueryForDatabase(queryName, substitutions, null);
    }
    /**
     * Same as {@link #getQuery(String,Map) above}, but allows a
     * different database to be specified.
     * @param queryName the name of the query.
     * @param substitutions a map of translations of substitution variables
     * @param dbspec the key under which to find the database url for
     * this query.
     **/
    public String getQueryForDatabase(String queryName, Map substitutions, String dbspec) {
        String dbtype = default_dbtype;;
        if (dbspec != null) {
            String dburl = getProperty(dbspec);
            if (dburl != null) {
                dbtype = getDBType(dburl);
            }
        }
        String result = getQuery1(queryName + "." + dbtype, substitutions);
        if (result == null) result = getQuery1(queryName, substitutions);
        if (result == null)
            throw new IllegalArgumentException("No query named " + queryName);
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
                    if (subst == null)
                        throw new IllegalArgumentException("Null value for " + key);
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
        return name;
    }
}
