/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.core.component.ComponentDescription;
import org.cougaar.core.component.Service;

/** 
 * Agent's "containment service" that allows a child component
 * of the agent to interact with the agent's container (add, 
 * remove, etc).
 * <p>
 * For example, a plugin within an agent can add another 
 * brand-new plugin to its agent by using this service.
 * <p>
 * Each agent has its own containment service that only
 * applies to that agent's contents -- this service can not
 * be used to add a component to a different agent.
 */
public interface AgentContainmentService extends Service {

  boolean add(ComponentDescription desc);

  boolean remove(ComponentDescription desc);

  boolean contains(ComponentDescription desc);

  // add "list" enhancements here -- see bug 1113

}
