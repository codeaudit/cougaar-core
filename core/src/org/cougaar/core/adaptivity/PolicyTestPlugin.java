/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.adaptivity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.util.ConfigFinder;

/**
 * Test plugin that alternately constrains and unconstrains
 * the playbook with an operating mode policy it read froma file
 */
public class PolicyTestPlugin extends ServiceUserPlugin {
  private PlaybookConstrainService playbookConstrainService;

  private boolean constrained = false;

  private OperatingModePolicy[] policies;

  private static final Class[] requiredServices = {
    PlaybookConstrainService.class
  };

  public PolicyTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    String policyFileName = getParameters().iterator().next().toString();
    try {
      Reader is = new InputStreamReader(getConfigFinder().open(policyFileName));
      try {
        Parser p = new Parser(is, logger);
        policies = p.parseOperatingModePolicies();
      } finally {
        is.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing policy file", e);
    }
    if (haveServices()) setPolicies();
  }

  private boolean haveServices() {
    if (playbookConstrainService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      playbookConstrainService = (PlaybookConstrainService)
        sb.getService(this, PlaybookConstrainService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setPolicies();
      }
    }
  }

  private void setPolicies() {
    if (true || constrained) {
      if (logger.isInfoEnabled()) logger.info("Adding threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        playbookConstrainService.constrain(policies[i]);
      }
      constrained = false;
    } else {
      if (logger.isInfoEnabled()) logger.info("Removing threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        playbookConstrainService.unconstrain(policies[i]);
      }
      constrained = true;
    }
    startTimer(75000);
  }
}
