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

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.UnaryPredicate;

public abstract class FreezePlugin extends ServiceUserPlugin {
  protected LoggingService logger;
  protected FreezePlugin() {
    super(new Class[0]);
  }

  public void setupSubscriptions() {
    logger = (LoggingService) getServiceBroker()
      .getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(logger, getMessageAddress().toString() + ": ");

  }

  protected static UnaryPredicate targetRelayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof FreezeRelayTarget;
      }
    };

  protected static UnaryPredicate sourceRelayPredicate =
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return o instanceof FreezeRelaySource;
      }
    };
}
