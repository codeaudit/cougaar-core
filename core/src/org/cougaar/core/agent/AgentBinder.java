/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;

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

}
