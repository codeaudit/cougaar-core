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

package org.cougaar.core.plugin;

import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.blackboard.TimestampEntry;
import org.cougaar.core.blackboard.TimestampSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.BlackboardTimestampService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.UnaryPredicate;

/**
 * The TimestampServicePlugin is a plugin that provides the
 * BlackboardTimestampService to other plugins and components.
 * <p>
 * This classes provides access to a shared 
 * TimestampSubscription's contents.
 */
public final class TimestampServicePlugin
extends ComponentPlugin
{
  
  private static final UnaryPredicate PRED =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        // for now accept all unique objects
        //
        // could filter for just Tasks and PlanElements
        return (o instanceof UniqueObject);
      }
    };

  private final TimestampSubscription timeSub = 
    new TimestampSubscription(PRED);

  private BlackboardTimestampServiceProvider btSP;

  public void load() {
    super.load();
    if (btSP ==  null) {
      btSP = new BlackboardTimestampServiceProvider();
      getServiceBroker().addService(BlackboardTimestampService.class, btSP);
    }
  }

  public void unload() {
    if (btSP != null) {
      getServiceBroker().revokeService(BlackboardTimestampService.class, btSP);
      btSP = null;
    }
    super.unload();
  }

  protected void setupSubscriptions() {
    Object sub = blackboard.subscribe(timeSub);
    if (sub != timeSub) {
      throw new RuntimeException(
          "Subscribe returned a different subscription?");
    }
  }
  
  protected void execute() {
    // never, since we never register to watch the changes
  }

  private class BlackboardTimestampServiceProvider
  implements ServiceProvider {

    // for now we can share a single service instance
    private BlackboardTimestampServiceImpl INSTANCE =
      new BlackboardTimestampServiceImpl();

    public Object getService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass) {
      if (serviceClass == BlackboardTimestampService.class) {
        return INSTANCE;
      } else {
        throw new IllegalArgumentException(
            this+" does not provide a service for: "+serviceClass);
      }
    }

    public void releaseService(
        ServiceBroker sb, 
        Object requestor, 
        Class serviceClass, 
        Object service)  {
      // ignore
    }

    private class BlackboardTimestampServiceImpl
      implements BlackboardTimestampService {
        public long getCreationTime(UID uid) {
          return timeSub.getCreationTime(uid);
        }
        public long getModificationTime(UID uid) {
          return timeSub.getModificationTime(uid);
        }
        public TimestampEntry getTimestampEntry(UID uid) {
          return timeSub.getTimestampEntry(uid);
        }
      }
  }
  
}
