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

package org.cougaar.core.plugin.freeze;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.WeakHashMap;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.TopologyReaderService;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates freeze information from
 * agents in a node to determine the "freeze" of the current
 * tasks. It continually determines the worst laggard in the node and
 * forwards that one laggard to the society root.
 **/

public class FreezeNodePlugin extends FreezeSourcePlugin {
  private IncrementalSubscription relaySubscription;

  public void setupSubscriptions() {
    super.setupSubscriptions();
    relaySubscription = (IncrementalSubscription)
      blackboard.subscribe(targetRelayPredicate);
  }

  /**
   * If the relay subscription becomes empty with thaw our children
   * (rescind our relay). If the relay subscription becomes non-empty,
   * we freeze our children (send a relay). Redundant freezes and
   * thaws are filtered by our base class.
   */
  public void execute() {
    if (relaySubscription.hasChanged()) {
      if (relaySubscription.isEmpty()) {
        thaw();                 // Thaw if frozen
      } else {
        freeze();
      }
    }
    super.execute();
  }

  // Implement abstract methods
  /**
   * Get the names of our target agents.
   * @return the names of agents in this node
   **/
  protected Set getTargetNames() {
    return topologyReaderService
      .getChildrenOnParent(TopologyReaderService.AGENT,
                           TopologyReaderService.NODE,
                           getMessageAddress().toString());
  }

  /**
   * Our children have become frozen, so we tell our parent(s) we are frozen, too
   **/
  protected void setUnfrozenAgents(Set unfrozenAgents) {
    if (logger.isDebugEnabled()) logger.debug("unfrozen " + unfrozenAgents);
    for (Iterator i = relaySubscription.iterator(); i.hasNext(); ) {
      FreezeRelayTarget relay = (FreezeRelayTarget) i.next();
      relay.setUnfrozenAgents(unfrozenAgents);
      blackboard.publishChange(relay);
    }
  }
}
