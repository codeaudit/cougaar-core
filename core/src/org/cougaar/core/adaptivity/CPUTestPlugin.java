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
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SensorMeasurementService;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.GenericStateModelAdapter;

public class CPUTestPlugin extends ServiceUserPluginBase {
  public static final String CPU_SENSOR_MEASUREMENT_NAME = "SensorMeasurement.CPU";

  private static final OMSMRange[] CPU_RANGES = {
    new OMSMRange(0.0, 1.0)
  };

  private static final OMSMValueList CPU_VALUES = new OMSMValueList(CPU_RANGES);

  private SensorMeasurementService sensorMeasurementService;

  private LoggingService logger;

  private static final Double[] cpuValues = {
    new Double(0.4),
  };

  private int cpuStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base SensorMeasurement
   * class which has no setter method.
   **/
  private static class CPUTestSensorMeasurement extends SensorMeasurementImpl implements NotPersistable {
    public CPUTestSensorMeasurement(String name, OMSMValueList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    LoggingService.class,
    SensorMeasurementService.class
  };

  public CPUTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    CPUTestSensorMeasurement cpu =
      new CPUTestSensorMeasurement(CPU_SENSOR_MEASUREMENT_NAME, CPU_VALUES, cpuValues[0]);
    getBlackboardService().publishAdd(cpu);
    if (haveServices()) setCPUSensor();
  }

  private boolean haveServices() {
    if (logger != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      logger = (LoggingService)
        sb.getService(this, LoggingService.class, null);
      sensorMeasurementService = (SensorMeasurementService)
        sb.getService(this, SensorMeasurementService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setCPUSensor();
      }
    }
  }

  private void setCPUSensor() {
    CPUTestSensorMeasurement cpu = (CPUTestSensorMeasurement)
      sensorMeasurementService.getSensorMeasurementByName(CPU_SENSOR_MEASUREMENT_NAME);
    if (cpu != null) {
      if (logger.isInfoEnabled()) logger.info("Setting cpu = " + cpuValues[cpuStep]);
      cpu.setValue(cpuValues[cpuStep]);
      getBlackboardService().publishChange(cpu);
      cpuStep++;
      if (cpuStep == cpuValues.length) cpuStep = 0;
    }
    startTimer(60000);
  }
}
