/*
 * <copyright>
 *  Copyright 2001,2002 BBNT Solutions, LLC
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
package org.cougaar.core.plugin;

import org.cougaar.core.component.BindingSite;

import org.cougaar.core.blackboard.BlackboardClientComponent;

import org.cougaar.util.ConfigFinder;

/**
 * Standard base class for Plugins.
 * <p>
 * Create a derived class by implementing 
 * <tt>setupSubscriptions()</tt> and <tt>execute()</tt>.
 * <p>
 * Note that both "precycle()" and "cycle()" will be run by the
 * scheduler.  This means that the scheduling order <i>in relation to 
 * other scheduled Components</i> may be *random* (i.e. this 
 * ComponentPlugin might load first but be precycled last!).  In 
 * general a Component should <b>not</b> make assumptions about the 
 * load or schedule ordering.
 */
public abstract class ComponentPlugin 
  extends BlackboardClientComponent
  implements PluginBase
{
  public ComponentPlugin() { 
  }
  
  /**
   * Binding site is set by reflection at creation-time.
   */
  public void setBindingSite(BindingSite bs) {
    if (bs instanceof PluginBindingSite) {
      super.setBindingSite(bs);
    } else {
      throw new RuntimeException("Tried to load "+this+" into "+bs);
    }
  }

  // 10.0: Old method PluginBindingSite getBindingSite() is gone!
  // However, callers may cast the return of getBindingSite()
  // if _absolutely_ necessary.
  // If you want the AgentIdentifier, you may simply call getAgentIdentifier()
  // -- inherited from BlackboardClientComponent

  /**
   * Called once after initialization, as a "pre-execute()".
   */
  protected abstract void setupSubscriptions();
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected abstract void execute();
  
  //
  // misc utility methods:
  //

  protected ConfigFinder getConfigFinder() {
    return ((PluginBindingSite) getBindingSite()).getConfigFinder();
  }
  
}
