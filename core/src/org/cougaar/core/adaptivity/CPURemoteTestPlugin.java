/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A plugin to exercise the Relay mechanism by setting a
 * RelayedOperatingMode that is targeted to the Provider agent. The
 * target manifestation of the operating mode is a Sensor that is used
 * by the adaptivity engine to select plays.
 **/
public class CPURemoteTestPlugin extends ServiceUserPlugin {
  /** The name of the OperatingMode and Condition **/
  public static final String CPU_CONDITION_NAME = "CPURemoteTestPlugin.CPU";

  /** A range from 0.0 thru 1.0 **/
  private static final OMCRange[] CPU_RANGES = {new OMCRange(0.0, 1.0)};

  /** A value list with just one range from 0.0 thru 1.0 **/
  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  private UIDService uidService;

  private InterAgentCondition cpu;

  private static final Double[] cpuValues = {
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(1.0),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
    new Double(0.1),
  };

  private int cpuStep = 0;

  private static final Class[] requiredServices = {
    UIDService.class
  };

  public CPURemoteTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    cpu = new InterAgentCondition(CPU_CONDITION_NAME,
                                      CPU_VALUES, cpuValues[0]);
    cpu.setTarget(MessageAddress.getMessageAddress("Provider"));
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) {
      uidService.registerUniqueObject(cpu);
      setCPUCondition();
    }
  }

  private boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = (UIDService)
        sb.getService(this, UIDService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setCPUCondition();
      }
    }
  }

  private void setCPUCondition() {
    if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
    cpu.setValue(cpuValues[cpuStep]);
    getBlackboardService().publishChange(cpu);
    cpuStep++;
    if (cpuStep == cpuValues.length) cpuStep = 0;
    startTimer(60000);
  }
}
