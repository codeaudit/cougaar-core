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

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.OperatingModeService;

public class LateOperatingModePlugin extends ServiceUserPlugin {
  public static final String OM_NAME = "LateOperatingModePlugin.LATE";

  private static final OMCRange[] RANGES = {
    new OMCRange(0.0, 1.0)
  };

  private static final OMCRangeList VALUES = new OMCRangeList(RANGES);

  private static final Class[] requiredServices = {
    OperatingModeService.class
  };

  private OperatingModeService omService;
  private OperatingMode om;
  private int step = 0;

  public LateOperatingModePlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    if (haveServices()) check();
  }

  private boolean haveServices() {
    if (omService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      omService = (OperatingModeService)
        sb.getService(this, OperatingModeService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) check();
    }
  }

  private void check() {
    cancelTimer();
    if (++step >= 4) {
      if (om == null) {
        om = new OperatingModeImpl(OM_NAME, VALUES, new Double(0.0));
        blackboard.publishAdd(om);
        logger.info("Publishing " + om);
      } else {
        logger.info("Checking " + om);
      }
    } else {
      logger.info("Waiting step " + step);
    }
    startTimer(5000);
  }
}
