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

package org.cougaar.core.plugin;

import java.io.PrintStream;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.service.AlarmService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;

/**
 * Convenience base class for plugins that need to acquire services
 * that may not be immediately available when first started. Records
 * the service classes needed and attempts acquire them on request.
 * <p>Also provides timer services.
 **/
public abstract class ServiceUserPlugin extends ComponentPlugin {
  private Class[] serviceClasses;

  private boolean[] serviceAcquired;

  private boolean allServicesAcquired = false;

  private Alarm timer;

  /**
   * Everybody needs a logger, so we provide it here.
   **/
  private LoggingService loggingService;
  protected LoggingService logger;

  /**
   * Constructor
   * @param serviceClasses the service classes needed for this plugin
   * to operate.
   **/
  protected ServiceUserPlugin(Class[] serviceClasses) {
    this.serviceClasses = serviceClasses;
    this.serviceAcquired = new boolean[serviceClasses.length];
  }

  /**
   * Override to get a logger on load
   **/
  public void load() {
    super.load();
    loggingService = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(loggingService, getMessageAddress().toString() + ": ");
  }

  /**
   * Override to release a logger on load
   **/
  public void unload() {
    if (loggingService != null) {
      getServiceBroker().releaseService(this, LoggingService.class, loggingService);
      logger = null;
      loggingService = null;
    }
    super.unload();
  }

  /**
   * Test if all services specified in the constructor are available.
   * Sub-classes should call this method from their setupSubscriptions
   * and execute methods until it returns true. Once this method
   * returns true, the services should be requested and normal
   * operation started. Once all the services are available, this
   * method simply returns true. See <code>haveServices()</code> in
   * {@link org.cougaar.core.adaptivity.ConditionServiceProvider#execute ConditionServiceProvider}
   * for a typical usage pattern.
   **/
  protected boolean acquireServices() {
    cancelTimer();
    if (!allServicesAcquired) {
      allServicesAcquired = true; // Assume we will get them all
      ServiceBroker sb = getServiceBroker();
      for (int i = 0; i < serviceClasses.length; i++) {
        if (!serviceAcquired[i]) {
          if (sb.hasService(serviceClasses[i])) {
            if (logger.isDebugEnabled()) {
//               logger.debug(serviceClasses[i].getName() + " acquired");
            }
            Object o = sb.getService(this, serviceClasses[i], null);
            if (o == null) {
              System.out.println(serviceClasses[i].getName() + " exists but is unavailable");
              allServicesAcquired = false;
            } else {
              sb.releaseService(this, serviceClasses[i], o);
              serviceAcquired[i] = true;
            }
          } else {
            if (logger.isDebugEnabled()) {
//               logger.debug(serviceClasses[i].getName() + " missing");
            }
            allServicesAcquired = false;
          }
        }
      }
      if (!allServicesAcquired) {
        startTimer(1000L);
      }
    }
    return allServicesAcquired;
  }

  /**
   * Test if the timer has expired.
   * @return false if the timer is not running or has not yet expired
   * else return true.
   **/
  protected boolean timerExpired() {
    return timer != null && timer.hasExpired();
  }

  /**
   * Schedule a update wakeup after some interval of time
   * @param delay how long to delay before the timer expires.
   **/
  protected void startTimer(final long delay) {
    if (timer != null) return;  // update already scheduled
//     if (logger.isDebugEnabled()) logger.debug("Starting timer " + delay);
    if (getBlackboardService() == null && 
        logger != null && 
        logger.isWarnEnabled()) {
      logger.warn(
          "Started service alarm before the blackboard service"+
          " is available");
    }
    timer = new Alarm() {
      long expirationTime = System.currentTimeMillis() + delay;
      boolean expired = false;
      public long getExpirationTime() {return expirationTime;}
      public synchronized void expire() {
        if (!expired) {
          expired = true;
          BlackboardService bb = getBlackboardService();
          if (bb != null) {
            bb.signalClientActivity();
          } else {
            if (logger != null && logger.isWarnEnabled()) {
              logger.warn(
                  "Alarm started "+delay+" millis ago has expired,"+
                  " but the blackboard service is null.  Plugin "+
                  " model state is "+getModelState());
            }
          }
        }
      }
      public boolean hasExpired() { return expired; }
      public synchronized boolean cancel() {
        boolean was = expired;
        expired=true;
        return was;
      }
    };
    getAlarmService().addRealTimeAlarm(timer);
  }

  /**
   * Cancel the timer.
   **/
  protected void cancelTimer() {
    if (timer == null) return;
//     if (logger.isDebugEnabled()) logger.debug("Cancelling timer");
    timer.cancel();
    timer = null;
  }
}
