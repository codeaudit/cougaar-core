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

import org.cougaar.core.component.BinderFactory;
import org.cougaar.core.component.BinderSupport;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ContainerAPI;
import org.cougaar.core.mts.MessageAddress;

/**
 * The default Binder for Agents.
 */
public class DefaultAgentBinder 
extends BinderSupport 
implements AgentBinder, AgentBindingSite
{
  /** All subclasses must implement a matching constructor. **/
  public DefaultAgentBinder(BinderFactory bf, Object child) {
    super(bf, child);
  }

  public final MessageAddress getAgentIdentifier() {
    return getAgent().getAgentIdentifier();
  }

  public final Agent getAgent() {
    return (Agent) getComponent();
  }

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
}
