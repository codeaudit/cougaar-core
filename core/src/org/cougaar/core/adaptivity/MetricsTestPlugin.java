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
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.plugin.ServiceUserPlugin;
import org.cougaar.core.qos.metrics.Metric;
import org.cougaar.core.qos.metrics.MetricsService;
import org.cougaar.core.service.ConditionService;

public class MetricsTestPlugin extends ServiceUserPlugin {
  public static final String JIPS_CONDITION_NAME = "MetricsTestPlugin.JIPS";

  private static final OMCRange[] JIPS_RANGES = {
    new OMCRange(0.0, Double.MAX_VALUE)
  };

  private static final OMCRangeList JIPS_VALUES = new OMCRangeList(JIPS_RANGES);

  private ConditionService conditionService;

  private MetricsService metricsService;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class MetricsTestCondition extends SensorCondition implements NotPersistable {
    public MetricsTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class,
    MetricsService.class
  };

  public MetricsTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    MetricsTestCondition jips =
      new MetricsTestCondition(JIPS_CONDITION_NAME, JIPS_VALUES, new Double(1.0));
    getBlackboardService().publishAdd(jips);
    if (haveServices()) setMetricsConditions();
  }

  private boolean haveServices() {
    if (conditionService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      conditionService = (ConditionService)
        sb.getService(this, ConditionService.class, null);
      metricsService = (MetricsService)
        sb.getService(this, MetricsService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setMetricsConditions();
      }
    }
  }

  private void setMetricsConditions() {
    logger.info("setMetricsConditions");
    // raw CPU capacity
    Metric metric =  metricsService.getValue("Agent(Provider):Jips");
    // Includes effects of load average, but different units
    // Metric metric =  svc.getValue("Agent(3ID):EffectiveMJips");
    MetricsTestCondition jips = (MetricsTestCondition)
      conditionService.getConditionByName(JIPS_CONDITION_NAME);
    if (metric != null) {
      if (jips != null) {
        Double value = new Double(metric.doubleValue());
        if (logger.isInfoEnabled()) logger.info("Setting jips = " + value);
        jips.setValue(value);
        getBlackboardService().publishChange(jips);
      } else {
        logger.warn("jips is null");
      }
    } else {
      logger.warn("metric is null");
    }
    resetTimer(10000);
  }
}
