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
package org.cougaar.core.servlet;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Utility methods that may assist in writing Servlets.
 */
public final class ServletUtil {

  private ServletUtil() {
    // just utility functions.
  }

  /**
   * Get the "/$name" encoded Agent name from the request path.
   */
  public static String getEncodedAgentName(
      HttpServletRequest request) 
  {
    String uri = request.getRequestURI();

    // return everything after the '$' & before the '/' 
    String name = new String();
    uri = uri.substring(uri.indexOf('$')+1);
    name = uri.substring(0, uri.indexOf('/'));
    return name;
  }

  /**
   * Get the path after the "/$name".
   */
  public static String getPath(
      HttpServletRequest request) 
  {
    // return everything beyond $name/ in the uri
    String uri = request.getRequestURI();
    int begin = uri.indexOf('/', 2);
    return uri.substring(begin);
  }

  /**
   * Make the GET and POST passing of parameters transparent to 
   * the user.
   * <p>
   * Determine either GET or POST methods, call with respective 
   * ServletUtil methods.
   *
   * @see ParamVisitor inner-class defined at the end of this class
   */
  public static void parseParams(
      ParamVisitor vis, 
      HttpServletRequest req) throws IOException
  {  
    String meth = req.getMethod();
    if (meth.equals("GET")) {
      // check for no query params
      if (req.getQueryString() != null) {
        Map m = HttpUtils.parseQueryString(req.getQueryString());
        parseParams(vis, m);
      }
    } else if (meth.equals("POST")) {
      int len = req.getContentLength();
      ServletInputStream in = req.getInputStream();
      Map m = HttpUtils.parsePostData(len, in);
      parseParams(vis, m);
    }
  }

  /**
   * Given a <code>Map</code> of (name, value) pairs, call back 
   * to the given <code>ParamVisitor</code>'s "setParam(name,value)"
   * method.
   *
   * @see ParamVisitor inner-class defined at the end of this class
   */
  public static void parseParams(
      ParamVisitor vis, 
      Map m) {
    Iterator iter = m.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry me = (Map.Entry)iter.next();
      String key = me.getKey().toString();
      String[] value_array = (String[])me.getValue();
      String value = value_array[0];
      vis.setParam(key, value);      
    }
  }

  /**
   * Simple callback API for use with "setParams(..)".
   */
  public interface ParamVisitor {
    void setParam(String key, String value);
  }

}
