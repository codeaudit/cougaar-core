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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.GenericStateModelAdapter;

public class CPUTestPlugin extends ServiceUserPlugin {
  public static final String CPU_CONDITION_NAME = "CPUTestPlugin.CPU";

  private static final OMCRange[] CPU_RANGES = {
    new OMCRange(0.0, 1.0)
  };

  private static final OMCRangeList CPU_VALUES = new OMCRangeList(CPU_RANGES);

  private ConditionService conditionService;

  private static final Double[] cpuValues = {
    new Double(0.0),
    new Double(0.1),
    new Double(0.2),
    new Double(0.3),
    new Double(0.4),
    new Double(0.5),
    new Double(0.6),
    new Double(0.7),
    new Double(0.8),
    new Double(0.9),
    new Double(0.8),
    new Double(0.7),
    new Double(0.6),
    new Double(0.7),
    new Double(0.8),
    new Double(0.9),
    new Double(0.8),
    new Double(0.7),
    new Double(0.6),
    new Double(0.5),
    new Double(0.4),
    new Double(0.3),
    new Double(0.2),
    new Double(0.1),
  };

  private int cpuStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class CPUTestCondition extends SensorCondition implements NotPersistable {
    public CPUTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public CPUTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    CPUTestCondition cpu =
      new CPUTestCondition(CPU_CONDITION_NAME, CPU_VALUES, cpuValues[0]);
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) setCPUCondition();
  }

  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      conditionService = (ConditionService)
        sb.getService(this, ConditionService.class, null);
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
    CPUTestCondition cpu = (CPUTestCondition)
      conditionService.getConditionByName(CPU_CONDITION_NAME);
    if (cpu != null) {
      if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
      cpu.setValue(cpuValues[cpuStep]);
      getBlackboardService().publishChange(cpu);
      cpuStep++;
      if (cpuStep == cpuValues.length) cpuStep = 0;
    }
    startTimer(5000);
  }
}
