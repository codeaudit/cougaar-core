/*
 * <copyright>
 *  Copyright 2001-2003 BBNT Solutions, LLC
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.UIDService;
import org.cougaar.util.UnaryPredicate;

/**
 * This plugin gathers and integrates freeze information from agents
 * in a society to determine the "completion" of the freeze operation.
 * In most agents, it gathers the information and forwards the frozen
 * status of the agent to another agent. This process continues
 * through a hierarchy of such plugins until the plugin at the root of
 * the tree is reached. When the root determines that ice has been
 * acheived, that is reflected in the freeze control servlet
 **/

public abstract class FreezeSourcePlugin extends FreezePlugin {
  protected UIDService uidService;
  private Subscription relaySubscription;
  private FreezeRelaySource relay; // The relay we sent

  public void unload() {
    if (uidService != null) {
      ServiceBroker sb = getServiceBroker();
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    super.unload();
  }

  public void setupSubscriptions() {
    super.setupSubscriptions();
    ServiceBroker sb = getServiceBroker();
    uidService = (UIDService)
      sb.getService(this, UIDService.class, null);
    relaySubscription = blackboard.subscribe(sourceRelayPredicate);
  }

  public void execute() {
    if (relaySubscription.hasChanged()) {
      if (relay != null) {
        setUnfrozenAgents(relay.getUnfrozenAgents());
      }
    }
  }

  protected abstract Set getTargetNames();

  protected abstract void setUnfrozenAgents(Set unfrozenAgents);

  protected synchronized void freeze() {
    if (relay != null) return;  // Already frozen
    if (logger.isDebugEnabled()) logger.debug("freeze");
    MessageAddress me = getAgentIdentifier();
    Set names = getTargetNames();
    Set targets = new HashSet(names.size());
    for (Iterator i = names.iterator(); i.hasNext(); ) {
      MessageAddress cid = MessageAddress.getMessageAddress((String) i.next());
      if (!cid.equals(me)) targets.add(cid);
    }
    relay = new FreezeRelaySource(targets);
    relay.setUID(uidService.nextUID());
    blackboard.publishAdd(relay);
    relaySubscription = blackboard.subscribe(new UnaryPredicate() {
        public boolean execute(Object o) {
          return o == relay;
        }
      });
    setUnfrozenAgents(names);
  }

  protected synchronized void thaw() {
    if (relay == null) return;  // not frozen
    if (logger.isDebugEnabled()) logger.debug("thaw");
    blackboard.publishRemove(relay);
    blackboard.unsubscribe(relaySubscription);
    relay = null;
  }
}

