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

import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.service.ConditionService;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.DynamicUnaryPredicate;

/**
 * Plugin to sense task processing conditions. We create and maintain
 * three Conditions: the rate of arrival of new tasks
 * (TaskSensor.publishRate), the rate of disposal of tasks
 * (TaskSensor.disposeRate), and the current backlog
 * (TaskSensor.backlog). The backlog is the integral of the difference
 * of the first two. The rate of disposal includes the rescind rate.
 * All three are smoothed with low-pass filters having the same time
 * constant (30 seconds currently).
 **/
public class TaskSensorPlugin extends ServiceUserPlugin {
  /** The name of the Condition we publish **/
  private static final String PUBLISH_RATE_CONDITION_NAME = "TaskSensor.publishRate";
  private static final String DISPOSE_RATE_CONDITION_NAME = "TaskSensor.disposeRate";
  private static final String BACKLOG_CONDITION_NAME      = "TaskSensor.backlog";

  private static final OMCRangeList POSITIVE_VALUES =
    new OMCRangeList(new OMCRange(0.0, Double.MAX_VALUE));

  private static final double TIME_CONSTANT = 30000.0; // Thirty second time constant
  private static final double UPDATE_INTERVAL = 10000.0; // Update every 10 seconds

  private ConditionService conditionService;

  private MyCondition publishRateCondition = new MyCondition(PUBLISH_RATE_CONDITION_NAME);
  private MyCondition disposeRateCondition = new MyCondition(DISPOSE_RATE_CONDITION_NAME);
  private MyCondition backlogCondition     = new MyCondition(BACKLOG_CONDITION_NAME);

  private long then = System.currentTimeMillis();
  private IncrementalSubscription backlogSubscription;
  private UnaryPredicate backlogPredicate = new DynamicUnaryPredicate() {
    public boolean execute(Object o) {
      if (o instanceof Task) {
        Task task = (Task) o;
        return task.getPlanElement() == null;
      }
      return false;
    }
  };

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class MyCondition extends SensorCondition implements NotPersistable {
    private double filtered = 0.0;

    public MyCondition(String name) {
      super(name, POSITIVE_VALUES);
      filtered = ((Number) getValue()).doubleValue();
    }

    public void update(double sample, long elapsed) {
      filtered = filtered / Math.exp(elapsed / TIME_CONSTANT) + sample;
    }

    public void publish() {
      super.setValue(new Double(filtered));
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public TaskSensorPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    blackboard.publishAdd(publishRateCondition);
    blackboard.publishAdd(disposeRateCondition);
    blackboard.publishAdd(backlogCondition);
    backlogSubscription = (IncrementalSubscription) blackboard.subscribe(backlogPredicate);
    if (haveServices()) update(true);
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
    logger.debug("execute");
    if (haveServices()) {
      update(timerExpired());
    }
  }

  /**
   * Update the conditions. We have one subscription to undisposed
   * tasks. From this subscription we obtain three values: The number
   * of newly published tasks is just the size of the added list of
   * the backlog subscription. The number of previously published
   * tasks that have been disposed is the size of the removed list of
   * the backlog subscription. And the current backlog is just the
   * size of the backlog subscription.
   **/
  private void update(boolean publish) {
    long now = System.currentTimeMillis();
    long elapsed = now - then;
    then = now;
    publishRateCondition.update(backlogSubscription.getAddedCollection().size(), elapsed);
    backlogCondition.update(backlogSubscription.size(), elapsed);
    disposeRateCondition.update(backlogSubscription.getRemovedCollection().size(), elapsed);
    if (publish) {
      cancelTimer();
      if (logger.isDebugEnabled()){
        logger.debug(publishRateCondition.toString());
        logger.debug(disposeRateCondition.toString());
        logger.debug(backlogCondition.toString());
      }
      publishRateCondition.publish();
      disposeRateCondition.publish();
      backlogCondition.publish();
      startTimer(UPDATE_INTERVAL);
    }
  }
}
