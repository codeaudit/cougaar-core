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
package org.cougaar.core.plugin;

import java.util.*;
import org.cougaar.core.relay.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.core.util.SimpleUniqueObject;
import org.cougaar.core.util.UID;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.mts.MessageAddress;

public class TestABA extends ComponentPlugin {
  private IncrementalSubscription relays;
  private UIDService uidService;
  private LoggingService logger;
  private MyRelay myRelay;
  private UnaryPredicate relayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof MyRelay;
      }
    };

  public void setUIDService(UIDService s) {
    uidService = s;
  }

  public void setLoggingService(LoggingService s) {
    logger = s;
    logger = LoggingServiceWithPrefix.add(logger, getMessageAddress() + ": ");
  }

  public void setupSubscriptions() {
    String cid = getMessageAddress().toString();
    boolean is135ARBN = cid.equals("1-35-ARBN");
    relays = (IncrementalSubscription) blackboard.subscribe(relayPredicate);
    if (is135ARBN) {
      logger.info("Adding relay at " + cid);
      AttributeBasedAddress target =
        AttributeBasedAddress.getAttributeBasedAddress("2-BDE-1-AD-COMM", "Role", "Member");
      myRelay = new MyRelay(Collections.singleton(target));
      myRelay.setUID(uidService.nextUID());
      blackboard.publishAdd(myRelay);
    } else {
      logger.debug("Waiting at " + cid);
    }
  }

  public void execute() {
    if (relays.hasChanged()) {
      int n = relays.size();
      printList("Added", relays.getAddedCollection(), n);
      printList("Removed", relays.getRemovedCollection(), n);
    }
  }

  private void printList(String msg, Collection list, int size) {
    for (Iterator i = list.iterator(); i.hasNext(); ) {
      MyRelay relay = (MyRelay) i.next();
      if (relay == myRelay) continue;
      MessageAddress src = relay.getSource();
      logger.info(msg + "(" + size + "): " + src);
    }
  }

  private static class MyRelay extends SimpleUniqueObject
    implements Relay.Source, Relay.Target, Relay.TargetFactory
  {
    transient Set targets;
    transient MessageAddress source;

    MyRelay(Set targets) {
      this.targets = targets;
    }
    MyRelay(UID uid, MessageAddress src) {
      this.targets = Collections.EMPTY_SET;
      this.source = src;
      setUID(uid);
    }
    public Set getTargets() {
      return targets;
    }
    public Object getContent() {
      return this;
    }

    public TargetFactory getTargetFactory() {
      return this;
    }
    public int updateResponse(MessageAddress target, Object response) {
      return NO_CHANGE;
    }
    public MessageAddress getSource() {
      return source;
    }
    public Object getResponse() {
      return null;
    }
    public int updateContent(Object content, Token token) {
      return CONTENT_CHANGE;
    }
    public Relay.Target create(UID uid, MessageAddress source, Object content, Token token)
    {
      return new MyRelay(uid, source);
    }
  }
}
