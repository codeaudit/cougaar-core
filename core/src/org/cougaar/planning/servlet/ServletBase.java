/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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
 
package org.cougaar.planning.servlet;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import org.cougaar.core.servlet.SimpleServletSupport;

/**
 * <pre>
 * Base class for all servlets in the datagrabber.
 *
 * Holds the simple servlet support object.
 * Automatically treats POST requests as GETs.
 * Defines the usage function as something to be called when there
 * are no url parameters.
 *
 * Most of the work of a servlet is done by the ServletWorker.
 * A new ServletWorker is created for every new request.
 * </pre>
 */
public abstract class ServletBase
  extends HttpServlet {
  public static final boolean DEBUG = false;

  public static boolean VERBOSE = false;

  static {
    VERBOSE = Boolean.getBoolean("org.cougaar.mlm.ui.psp.transit.ServletBase.verbose");
  }

  /**
   * Save our service broker during initialization.
   */
  protected SimpleServletSupport support;
  public SimpleServletSupport getSupport () { return support; }

  /** **/
  public ServletBase(SimpleServletSupport support) {
    this.support = support;
  }

  /**
   * Pretty to-String for debugging.
   */
  public String toString() {    return getClass().getName();  }

  public void doGet(HttpServletRequest request,
		    HttpServletResponse response) throws IOException, ServletException {
    ServletWorker worker = createWorker ();
    if (!request.getParameterNames().hasMoreElements ()) {
      getUsage (response.getWriter(), support);
      return;
    }
    if (VERBOSE) {
      Enumeration paramNames = request.getParameterNames();
      for (int i = 0; paramNames.hasMoreElements (); )
	System.out.println ("ServletBase got param #" + i++ + " - " + paramNames.nextElement ());
    }
    worker.execute (request, response, support);
  }

  public void doPost(HttpServletRequest request,
		     HttpServletResponse response) throws IOException, ServletException {
    doGet (request, response);
  }

  protected abstract ServletWorker createWorker ();

  /** 
   * USAGE <p>
   *
   * Only called if no arguments are given.
   */
  public abstract void getUsage (PrintWriter out, SimpleServletSupport support);
}

