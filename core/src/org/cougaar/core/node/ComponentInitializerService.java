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

package org.cougaar.core.node;

import org.cougaar.core.component.Service;
import org.cougaar.core.component.ComponentDescription;

/**
 * Component model configuration service.
 */
public interface ComponentInitializerService extends Service {

  /**
   * Get the descriptions of components with the named parent having
   * an insertion point <em>below</em> the given container insertion point.
   * <p> 
   * For example, to get items with insertion point Node.AgentManager.Agent,
   * pass in Node.AgentManager. Then use 
   * <code>ComponentDescriptions.extractInsertionPointComponent("Node.AgentManager.Agent")</code> 
   * to get just those with the required insertion point. Typical
   * usage however is for the Node.AgentManager component to pass in its
   * own insertion point as a way to find its child components. 
   * <p>
   * Note that the returned value is in whatever order the underlying 
   * implementation uses.  It is <em>not</em> sorted by priority.
   **/
  ComponentDescription[] getComponentDescriptions(
      String parentName, String insertionPoint)
    throws InitializerException;

  /**
   * Generic exception for component initializer failures.
   */
  public class InitializerException extends Exception {
    public InitializerException(String msg, Throwable t) {
      super(msg, t);
    }
  }
}
