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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.SensorMeasurementService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.KeyedSet;
import org.cougaar.util.ReadOnlySet;
import org.cougaar.core.component.BindingSite;

/**
 * This Componentserves as an OperatingModeServiceProvider
 **/
public class SensorMeasurementServiceProvider
    extends ComponentPlugin
    implements ServiceProvider
{
  private static class SensorMeasurementSet extends KeyedSet {
    public SensorMeasurementSet() {
      super();
      makeSynchronized();
    }

    protected Object getKey(Object o) {
      SensorMeasurement sm = (SensorMeasurement) o;
      return sm.getName();
    }

    public SensorMeasurement getSensorMeasurement(String name) {
      return (SensorMeasurement) inner.get(name);
    }
  }

  private class SensorMeasurementServiceImpl implements SensorMeasurementService {
    public SensorMeasurement getSensorMeasurementByName(String knobName) {
      return smSet.getSensorMeasurement(knobName);
    }

    public Set getAllSensorMeasurementNames() {
      return new ReadOnlySet(smSet.keySet());
    }

    public void addListener(Listener l) {
      listeners.add(l);
    }

    public void removeListener(Listener l) {
      listeners.remove(l);
    }
  }

  private static UnaryPredicate SensorMeasurementPredicate = new UnaryPredicate() {
    public boolean execute(Object o) {
      return o instanceof SensorMeasurement;
    }
  };

  private SensorMeasurementSet smSet = new SensorMeasurementSet();
  private IncrementalSubscription sensorMeasurements;

  private List listeners = new ArrayList(2);

  private LoggingService logger;

  public void load() {
    super.load();
    logger = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    getServiceBroker().addService(SensorMeasurementService.class, this);
  }

  public void unload() {
    getServiceBroker().releaseService(this, LoggingService.class, logger);
    getServiceBroker().revokeService(SensorMeasurementService.class, this);
    super.unload();
  }

  public void setupSubscriptions() {
    sensorMeasurements = (IncrementalSubscription)
      getBlackboardService().subscribe(SensorMeasurementPredicate, smSet);
  }

  public void execute() {
    if (sensorMeasurements.hasChanged()) fireListeners();
  }

  private void fireListeners() {
    BlackboardService bb = getBlackboardService();
    if (logger.isDebugEnabled()) {
      for (Iterator i = sensorMeasurements.getAddedCollection().iterator(); i.hasNext(); ) {
        logger.debug("SensorMeasurement added: " + i.next());
      }
      for (Iterator i = sensorMeasurements.getChangedCollection().iterator(); i.hasNext(); ) {
        logger.debug("SensorMeasurement changed: " + i.next());
      }
      for (Iterator i = sensorMeasurements.getRemovedCollection().iterator(); i.hasNext(); ) {
        logger.debug("SensorMeasurement removed: " + i.next());
      }
      logger.debug("SensorMeasurementServiceProvider.fireListeners:");
    }
    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
      Object o = i.next();
      if (logger.isDebugEnabled()) logger.debug("    " + o);
      bb.publishChange(o);
    }
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == SensorMeasurementService.class) {
      return new SensorMeasurementServiceImpl();
    }
    throw new IllegalArgumentException(getClass() + " does not furnish "
                                       + serviceClass);
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object svc)
  {
    if (serviceClass != SensorMeasurementService.class
        || svc.getClass() != SensorMeasurementServiceImpl.class) {
      throw new IllegalArgumentException(getClass() + " did not furnish " + svc);
    }
  }
}
