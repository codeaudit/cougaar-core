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

package org.cougaar.core.service;

import javax.servlet.Servlet;
import org.cougaar.core.component.Service;

/**
 * Service that can be used to register a <code>Servlet</code>
 * for all HTTP requests that match a specified String path.
 * <p>
 * This is analogous to a <code>java.util.Map</code>:<pre>
 *   - register with "put(name, value)"
 *   - unregister with "remove(name)"
 *   - unregister-all with "clear()"
 * </pre>
 * <p>
 * "Servlet.init(..)" will be called upon <tt>register</tt>
 * and "Servlet.destroy()" will be called upon <tt>unregister</tt>.
 * Note that a <i>dummy</i> ServletConfig implementation will
 * be passed in both cases, where most methods return <tt>null</tt>.
 * This is done partially as a security precaution...
 * <p>
 * Also note that multiple concurrent "Servlet.service(..)" calls 
 * can occur, so your Servlet must be reentrant.
 * <p>
 * See:
 *  <a href=
 *  "http://java.sun.com/docs/books/tutorial/servlets">
 *   http://java.sun.com/docs/books/tutorial/servlets</a> for
 * tutorials on how to write Servlets and HttpServlets.
 *
 * @see org.cougaar.lib.web.service.LeafServletServiceComponent
 */
public interface ServletService extends Service
{

  /** 
   * Register a path to call the given <code>Servlet</code>.
   * <p>
   * This method will throw an <code>IllegalArgumentException</code>
   * if the path has already been registered by another ServletService
   * instance..
   * 
   * @see #unregister(String)
   */
  void register(
      String path,
      Servlet servlet) throws Exception;

  /** 
   * Unregister the <code>Servlet</code> with the specified path.
   * <p>
   * This method can only be used to unregister Servlets that have 
   * been registered with <b>this</b> service instance.
   *
   * @see #register(String,Servlet)
   * @see #unregisterAll()
   */
  void unregister(
      String path);

  /**
   * Unregister all <code>Servlet</code>s that have been registered
   * by <b>this</b> registration service instance.
   * <p>
   * This can be used as a one-stop cleanup utility.
   *
   * @see #unregister(String)
   */
  void unregisterAll();

  /**
   * Get the HTTP port for the local servlet server.
   * <p>
   * A typical HTTP port is 8800, but a different port may be
   * used due to either node configuration or to support multiple
   * nodes on the same machine.
   *
   * @return the HTTP port, or -1 if HTTP is disabled.
   */
  int getHttpPort();

  /**
   * Get the HTTPS port for the local servlet server.
   * <p>
   * A typical HTTPS port is 8400, but a different port may be
   * used due to either node configuration or to support multiple
   * nodes on the same machine.
   *
   * @return the HTTPS port, or -1 if HTTPS is disabled.
   */
  int getHttpsPort();
}
