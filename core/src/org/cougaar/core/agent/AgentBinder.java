/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.agent.*;

/** The standard Binder for Agents.
 **/
public class AgentBinder extends BinderSupport implements AgentBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public AgentBinder(BinderFactory bf, Object child) {
    super(bf, child);
  }

  protected final Agent getAgent() {
    return (Agent) getComponent();
  }
// protected final AgentManager getAgentManager() {
//     return (AgentManager)getContainer();
//   }
  protected final AgentManagerForBinder getAgentManager() {
    return (AgentManagerForBinder)getContainer();
  }
  protected final BindingSite getBinderProxy() {
    // horribly unsecure! Means that the component has full access to the binder.
    return (BindingSite) this;
  }

  public String toString() {
    return getAgent() + "'s AgentManagerBinder";
  }

  public String getName() {return getAgentManager().getName(); }
  public void registerAgent(Agent agent) { getAgentManager().registerAgent(agent); }
}
