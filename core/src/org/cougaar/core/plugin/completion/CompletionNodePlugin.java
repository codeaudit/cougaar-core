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

package org.cougaar.core.plugin.completion;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.Date;
import java.util.TimeZone;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.TopologyReaderService;

/**
 * This plugin gathers and integrates completion information from
 * agents in a node to determine the "completion" of the current
 * tasks. It continually determines the worst laggard in the node and
 * forwards that one laggard to the society root.
 **/

public class CompletionNodePlugin extends CompletionSourcePlugin {
  private IncrementalSubscription targetRelaySubscription;
  private Map filters = new WeakHashMap();

  public void setupSubscriptions() {
    targetRelaySubscription = (IncrementalSubscription)
      blackboard.subscribe(targetRelayPredicate);
    super.setupSubscriptions();
  }

  public void execute() {
    if (targetRelaySubscription.hasChanged()) {
      checkPersistenceNeeded(targetRelaySubscription);
    }
    super.execute();
  }

  protected Set getTargetNames() {
    return topologyReaderService
      .getChildrenOnParent(TopologyReaderService.AGENT,
                           TopologyReaderService.NODE,
                           getClusterIdentifier().toString());
  }

  protected void handleNewLaggard(Laggard newLaggard) {
    if (targetRelaySubscription.size() > 0) {
      for (Iterator i = targetRelaySubscription.iterator(); i.hasNext(); ) {
        CompletionRelay relay = (CompletionRelay) i.next();
        LaggardFilter filter = (LaggardFilter) filters.get(relay);
        if (filter == null) {
          filter = new LaggardFilter();
          filters.put(relay, filter);
        }
        if (filter.filter(newLaggard)) {
          if (logger.isDebugEnabled()) {
            logger.debug("setResponseLaggard " + newLaggard);
          }
          relay.setResponseLaggard(newLaggard);
          blackboard.publishChange(relay);
        } else {
          if (logger.isDebugEnabled()) logger.debug("No response ");
        }
      }
    } else {
      if (logger.isDebugEnabled()) logger.debug("No relays");
    }
  }
}
      
