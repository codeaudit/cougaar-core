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

package org.cougaar.planning.plugin.adaptivity;

import java.util.Collection;
import org.cougaar.core.adaptivity.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.ConditionService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 * Plugin to sense incoming task rate and publish a Condition
 **/
public class TaskRateSensorPlugin extends ServiceUserPlugin {
  private static final String CONDITION_NAME = "TaskRateSensorPlugin.TASKRATE";

  private static final OMCRangeList TASKRATE_VALUES = new OMCRangeList(
      new Double(0.0), new Double(Double.MAX_VALUE));

  private static final double TIME_CONSTANT = 5000.0; // Five second time constant

  private ConditionService conditionService;

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
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class TaskRateTestCondition extends SensorCondition implements NotPersistable {
    public TaskRateTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public TaskRateSensorPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    TaskRateTestCondition taskRate =
      new TaskRateTestCondition(CONDITION_NAME, TASKRATE_VALUES, new Double(0.0));
    blackboard.publishAdd(taskRate);
    tasksSubscription = (IncrementalSubscription) blackboard.subscribe(tasksPredicate);
    if (haveServices()) updateTaskRateSensor(true);
  }

  /**
   * Test if all needed services have been acquired. Test the
   * conditionService variable for null. If still null ask
   * acquireServices to continue trying to acquire services. If true
   * is returned, fill in the service variables and return true.
   * Subsequent calls will return true immediately.
   **/
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
      TaskRateTestCondition taskRate = (TaskRateTestCondition)
        conditionService.getConditionByName(CONDITION_NAME);
      if (taskRate != null) {
        if (logger.isInfoEnabled()) logger.info("Setting " + CONDITION_NAME + " = " + filteredTaskRate);
        taskRate.setValue(new Double(filteredTaskRate));
        blackboard.publishChange(taskRate);
      }
      startTimer(2000);
    }
  }
}
