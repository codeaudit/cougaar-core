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

public class ThreatconTestPlugin extends ServiceUserPlugin {
  public static final String THREATCON_CONDITION_NAME = "ThreatconTestPlugin.THREATCON";

  private static final OMCRange[] THREATCON_RANGES = {
    new OMCPoint("low"),
    new OMCPoint("high")
  };

  private static final OMCRangeList THREATCON_VALUES = new OMCRangeList(THREATCON_RANGES);

  private ConditionService conditionService;

  private static final String[] threatconValues = {"low", "high"};

  private int threatconStep = 0;

  /**
   * Private inner class precludes use by others to set our
   * measurement. Others can only reference the base Condition
   * class which has no setter method.
   **/
  private static class ThreatconTestCondition extends SensorCondition implements NotPersistable {
    public ThreatconTestCondition(String name, OMCRangeList allowedValues, Comparable value) {
      super(name, allowedValues, value);
    }

    public void setValue(Comparable newValue) {
      super.setValue(newValue);
    }
  }

  private static final Class[] requiredServices = {
    ConditionService.class
  };

  public ThreatconTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    ThreatconTestCondition threatcon =
      new ThreatconTestCondition(THREATCON_CONDITION_NAME, THREATCON_VALUES, threatconValues[0]);
    getBlackboardService().publishAdd(threatcon);
    if (haveServices()) setThreatconCondition();
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
        setThreatconCondition();
      }
    }
  }

  private void setThreatconCondition() {
    ThreatconTestCondition threatcon = (ThreatconTestCondition)
      conditionService.getConditionByName(THREATCON_CONDITION_NAME);
    if (threatcon != null) {
      if (logger.isInfoEnabled()) logger.info("Setting threatcon = " + threatconValues[threatconStep]);
      threatcon.setValue(threatconValues[threatconStep]);
      getBlackboardService().publishChange(threatcon);
      threatconStep++;
      if (threatconStep == threatconValues.length) threatconStep = 0;
    }
    startTimer(115000);
  }
}
