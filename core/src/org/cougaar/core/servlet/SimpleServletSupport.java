/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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
 
import java.util.Collection;
import java.util.List;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.ClusterIdentifier;

/**
 * This is a simple API for a <code>Servlet</code> to access
 * COUGAAR Services, such as the Blackboard.
 * <p>
 * This is for Servlets that are loaded by the
 * <code>SimpleServletComponent</code>.  See SimplerServletComponent
 * for loading details (".ini" usage, etc).
 * <p>
 * The Servlet class (e.g. "MyServlet") must have either a
 * "support" constructor:<pre>
 *    ... 
 *    public MyServlet(SimpleServletSupport support) {
 *      // save support for later use
 *    }
 *    ...</pre><br>
 * or the default constructor:<pre>
 *    ...
 *    public MyServlet() {
 *      // load with no support
 *    }
 *    ...</pre><br>
 * The default constructor can be used to load pure Servlets (i.e. 
 * Servlets without Cougaar references).
 * <p>
 *
 * @see SimpleServletComponent
 */
public interface SimpleServletSupport {

  /**
   * Get the path that this Servlet was loaded under.
   * <p>
   * This can also be obtained from the 
   * <tt>HttpServletRequest.getRequestURI()</tt>, which
   * will be "/$encoded-agent-name/path".
   */
  public String getPath();

  /**
   * Query the blackboard for all Objects that match the
   * given predicate.
   * <p>
   * Each call to "query" is a snapshot of the blackboard
   * that may contain different information than the last
   * "query" call, even within the same Servlet 
   * "service(..)" request.  The Objects returned should
   * be considered read-only!
   * <p>
   * This is the only blackboard access that is provided
   * to <i>simple</i> Servlets.  Servlets that need
   * to publish/subscribe/etc will require more complex
   * transaction open/close logic -- they should 
   * obtain the <code>ServletService</code> directly.
   * See <code>SimpleServletComponent</code> as a guide.
   */
  public Collection queryBlackboard(UnaryPredicate pred);

  /**
   * Get the URL- and HTML-safe (encoded) name of the
   * Agent that contains this Servlet instance.
   * <p>
   * Equivalent to 
   * <tt>encodeAgentName(getAgentIdentifier().getAddress())</tt>.
   * <p>
   * All "/$name/*" URLS must use the encoded Agent name.
   * In general the raw "agentName" may contain characters
   * that are not URL/HTML safe, such as:
   * <ul>
   *   <li>URL reserved characters  (" ", ":", "/", etc)</li>
   *   <li>HTML reserved characters ("&gt;", "&lt;")</li>
   *   <li>Arbitrary control characters ("\\n", "\\t", etc)</li>
   * </ul>
   *
   * @see #encodeAgentName
   */
  public String getEncodedAgentName();

  /**
   * Get the Agent's identifier for the Agent that contains
   * this Servlet instance.
   * <p>
   * The <tt>getAgentIdentifier().getAddress()</tt> is the 
   * "raw" name of the agent and may contain unsafe URL/HTML
   * characters.
   *
   * @see #getEncodedAgentName
   */
  public ClusterIdentifier getAgentIdentifier();

  /**
   * Equivalent to 
   * <tt>getAllEncodedAgentNames(new ArrayList())</tt>.
   */
  public List getAllEncodedAgentNames();

  /**
   * Fill the given <tt>toList</tt> with all the 
   * <code>String</code> encoded Agent names in the society.
   * <p>
   * This includes both local and remote Agent names.
   *
   * @see #getEncodedAgentName
   */
  public List getAllEncodedAgentNames(List toList);

  /**
   * Utility method to encode an Agent name -- 
   * equivalent to <tt>java.net.URLEncoder.encode(name)</tt>.
   *
   * @see #getEncodedAgentName
   */
  public String encodeAgentName(String name);

  //
  // add other COUGAAR-specific methods here.
  //
  // note that we want this to remain a *simple* API.
  //

  /*
  public NodeIdentifier getNodeIdentifier();
  */
}
