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
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SensorMeasurementService;
import org.cougaar.core.component.Service;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin to sense incoming task rate and publish a SensorMeasurement
 **/
public class TaskRateSensorPlugin extends ServiceUserPluginBase {
  private static final String SENSOR_MEASUREMENT_NAME = "SensorMeasurement.TASKRATE";

  private static final OMSMRange[] TASKRATE_RANGES = {
    new OMSMRange(0.0, Double.MAX_VALUE)
  };

  private static final OMSMValueList TASKRATE_VALUES = new OMSMValueList(TASKRATE_RANGES);

  private static final double TIME_CONSTANT = 5000.0; // Five second time constant

  private SensorMeasurementService sensorMeasurementService;

  private LoggingService logger;

  private double filteredTaskRate = 0.0;
  private long then = System.currentTimeMillis();
  private IncrementalSubscription tasksSubscription;
  private UnaryPredicate tasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return true;
      }
      return false;
    }
  };

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base SensorMeasurement
   * class which has no setter method.
   **/
  private static class TaskRateTestSensorMeasurement extends SensorMeasurementImpl implements NotPersistable {
    public TaskRateTestSensorMeasurement(String name, OMSMValueList allowedValues, Comparable value) {
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

  public TaskRateSensorPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    TaskRateTestSensorMeasurement taskRate =
      new TaskRateTestSensorMeasurement(SENSOR_MEASUREMENT_NAME, TASKRATE_VALUES, new Double(0.0));
    blackboard.publishAdd(taskRate);
    tasksSubscription = (IncrementalSubscription) blackboard.subscribe(tasksPredicate);
    if (haveServices()) updateTaskRateSensor(true);
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
    if (haveServices()) {
      updateTaskRateSensor(timerExpired());
    }
  }

  private void updateTaskRateSensor(boolean publish) {
    long now = System.currentTimeMillis();
    long elapsed = now - then;
    int newCount = tasksSubscription.getAddedCollection().size()
      + tasksSubscription.getChangedCollection().size()
      + tasksSubscription.getRemovedCollection().size();
    then = now;
    filteredTaskRate /= Math.exp(elapsed / TIME_CONSTANT);
    filteredTaskRate += newCount;
    if (publish) {
      cancelTimer();
      if (logger.isDebugEnabled()) logger.debug("newCount=" + newCount);
      TaskRateTestSensorMeasurement taskRate = (TaskRateTestSensorMeasurement)
        sensorMeasurementService.getSensorMeasurementByName(SENSOR_MEASUREMENT_NAME);
      if (taskRate != null) {
        if (logger.isInfoEnabled()) logger.info("Setting " + SENSOR_MEASUREMENT_NAME + " = " + filteredTaskRate);
        taskRate.setValue(new Double(filteredTaskRate));
        blackboard.publishChange(taskRate);
      }
      startTimer(2000);
    }
  }
}
