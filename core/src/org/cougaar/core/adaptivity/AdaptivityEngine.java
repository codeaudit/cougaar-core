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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PlaybookReadService;
import org.cougaar.core.service.SensorMeasurementService;
import org.cougaar.core.service.OperatingModeService;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.planning.ldm.policy.RangeRuleParameter;
import org.cougaar.planning.ldm.policy.RangeRuleParameterEntry;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;

/**
 * Sets OperatingModes for components based on plays in the playbook and
 * current sensor data
 */
public class AdaptivityEngine extends ServiceUserPluginBase {
  /**
   * A listener that listens to itself. It responds true when it is
   * itself the object of a subscription change.
   **/
  private static class Listener
    implements SensorMeasurementService.Listener,
               PlaybookReadService.Listener,
               UnaryPredicate
  {
    public boolean execute(Object o) {
      return (this == o);
    }
  };

  private PlayHelper helper;
  private PlaybookReadService playbookService;
  private OperatingModeService operatingModeService;
  private SensorMeasurementService sensorMeasurementService;

  private static Class[] requiredServices = {
    PlaybookReadService.class,
    OperatingModeService.class,
    SensorMeasurementService.class,
    LoggingService.class
  };

  private Subscription sensorMeasurementListenerSubscription;
  private Subscription playbookListenerSubscription;

  private Map smMap = new HashMap();

  private Play[] plays;

  private Set missingSensors = new HashSet();

  private Listener playbookListener = new Listener();

  private Listener sensorMeasurementListener = new Listener();

  public AdaptivityEngine() {
    super(requiredServices);
  }

  protected boolean haveServices() {
    if (playbookService != null) return true;
    if (acquireServices()) {
      playbookService = (PlaybookReadService)
        getServiceBroker().getService(this, PlaybookReadService.class, null);
      operatingModeService = (OperatingModeService)
        getServiceBroker().getService(this, OperatingModeService.class, null);
      sensorMeasurementService = (SensorMeasurementService)
        getServiceBroker().getService(this, SensorMeasurementService.class, null);
      sensorMeasurementService.addListener(sensorMeasurementListener);
      playbookService.addListener(playbookListener);
      helper = new PlayHelper(logger, operatingModeService, sensorMeasurementService, blackboard, smMap);
      return true;
    }
    return false;
  }

  public void stop() {
    ServiceBroker sb = getServiceBroker();
    if (playbookService != null) {
      playbookService.removeListener(playbookListener);
      sb.releaseService(this, PlaybookReadService.class, playbookService);
      playbookService = null;
    }
    if (sensorMeasurementService != null) {
      sensorMeasurementService.removeListener(sensorMeasurementListener);
      sb.releaseService(this, SensorMeasurementService.class, sensorMeasurementService);
      sensorMeasurementService = null;
    }
    if (operatingModeService != null) {
      sb.releaseService(this, OperatingModeService.class, operatingModeService);
      operatingModeService = null;
    }
    super.stop();
  }

  public void setupSubscriptions() {
    playbookListenerSubscription = blackboard.subscribe(playbookListener);
    sensorMeasurementListenerSubscription = blackboard.subscribe(sensorMeasurementListener);
    blackboard.publishAdd(sensorMeasurementListener);
    blackboard.publishAdd(playbookListener);
  }

  /**
   * The normal plugin execute. Wakes up whenever the playbook is
   * changed or whenever a SensorMeasurement is changed. Also wakes up
   * if the base class has set a timer waiting for all services to be
   * acquired. If the playbook has changed we refetch the new set of
   * plays and fetch the sensors required by those new plays. If the
   * playbook has not changed, but the sensor measurements have we
   * refetch the required sensors. If all required sensors are
   * available, the operating modes are updated from the current
   * plays.
   **/
  public synchronized void execute() {
    if (haveServices()) {
      if (plays == null || playbookListenerSubscription.hasChanged()) {
        if (logger.isDebugEnabled()) logger.debug("getting plays");
        getPlays(playbookService.getCurrentPlays());
        if (logger.isDebugEnabled()) logger.debug("getting sensor measurements");
        getSensorMeasurements();
      } else if (sensorMeasurementListenerSubscription.hasChanged()) {
        if (logger.isDebugEnabled()) logger.debug("getting sensor measurements");
        getSensorMeasurements();
      } else {
        if (logger.isDebugEnabled()) logger.debug("nothing changed");
      }
      if (missingSensors.size() == 0) {
        updateOperatingModes();
      }
    }
  }

  private void getPlays(Play[] newPlays) {
    plays = newPlays;
  }

  /**
   * Scan the current plays for required sensor measurements and stash
   * them in smMap for use in running the plays. Record the names of
   * any that are missing in the missingSensors Set.
   **/
  private void getSensorMeasurements() {
    smMap.clear();
    missingSensors.clear();
    for (int i = 0; i < plays.length; i++) {
      Play play = plays[i];
      for (Iterator x = play.getIfClause().iterator(); x.hasNext(); ) {
        Object o = x.next();
        if (o instanceof String) {
          String name = (String) o;
          if (!smMap.containsKey(name)) {
            SensorMeasurement sm = sensorMeasurementService.getSensorMeasurementByName(name);
            if (sm == null) {
              missingSensors.add(name);
            } else {
              smMap.put(name, sm);
            }
          }
        }
      }
    }
    if (logger.isDebugEnabled()) {
      if (missingSensors.size() > 0) {
        for (Iterator i = missingSensors.iterator(); i.hasNext(); ) {
          logger.debug("Sensor " + i.next() + " not availble");
        }
      } else {
        logger.debug("Have all required sensors");
      }
    }
  }

  /**
   * Update all operating modes based on sensor measurements and the
   * playbook. This is the real workhorse of this plugin and carries
   * out playbook-based adaptivity. All the active plays from the
   * playbook are considered. If the ifClause evaluates to true, then
   * the operating mode values are saved in a Map under the operating
   * mode name. When multiple plays affect the same operating mode,
   * the values are combined by intersecting the allowed value ranges.
   * If a play specifies a constraint that would have the effect of
   * eliminating all possible values for an operating mode, that
   * constraint is logged and ignored. Finally, the operating modes
   * are set to the effective value of the combined constraints.
   **/
  private void updateOperatingModes() {
    if (logger.isDebugEnabled()) logger.debug("updateOperatingModes");
    helper.updateOperatingModes(plays);
  }
}
