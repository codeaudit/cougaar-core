package org.cougaar.core.adaptivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.planning.ldm.policy.RangeRuleParameter;
import org.cougaar.planning.ldm.policy.RangeRuleParameterEntry;
import org.cougaar.planning.ldm.policy.RuleParameter;
import org.cougaar.planning.ldm.policy.RuleParameterIllegalValueException;

/**
 * Sets OperatingModes for components based on plays in the playbook and
 * current sensor data
 */

public abstract class ServiceUserPluginBase extends ComponentPlugin {
  private Class[] serviceClasses;

  private boolean[] serviceAcquired;

  private boolean allServicesAcquired = false;

  private Alarm timer;

  protected LoggingService logger;

  protected ServiceUserPluginBase(Class[] serviceClasses) {
    this.serviceClasses = serviceClasses;
    this.serviceAcquired = new boolean[serviceClasses.length];
  }

  public void load() {
    super.load();
    logger = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
  }

  public void unload() {
    if (logger != null) {
      getServiceBroker().releaseService(this, LoggingService.class, logger);
      logger = null;
    }
    super.unload();
  }

  protected boolean acquireServices() {
    cancelTimer();
    if (!allServicesAcquired) {
      allServicesAcquired = true; // Assume we will get them all
      ServiceBroker sb = getServiceBroker();
      for (int i = 0; i < serviceClasses.length; i++) {
        if (!serviceAcquired[i]) {
          Service svc = (Service) sb.getService(this, serviceClasses[i], null);
          if (svc == null) {
            allServicesAcquired = false;
            break;
          }
          if (logger.isDebugEnabled()) logger.debug(serviceClasses[i] + " acquired");
          serviceAcquired[i] = true;
          sb.releaseService(this, serviceClasses[i], svc);
        }
      }
      if (!allServicesAcquired) {
        startTimer(1000L);
      }
    }
    return allServicesAcquired;
  }

  protected boolean timerExpired() {
    return timer != null && timer.hasExpired();
  }

  /**
   * Schedule a update wakeup after some interval of time
   **/
  protected void startTimer(final long delay) {
    if (timer != null) return;  // update already scheduled
    if (logger.isDebugEnabled()) logger.debug("Starting timer " + delay);
    timer = new Alarm() {
      long expirationTime = System.currentTimeMillis() + delay;
      boolean expired = false;
      public long getExpirationTime() {return expirationTime;}
      public synchronized void expire() {
        if (!expired) {
          expired = true;
          getBlackboardService().signalClientActivity();
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

  protected void cancelTimer() {
    if (timer == null) return;
    if (logger.isDebugEnabled()) logger.debug("Cancelling timer");
    timer.cancel();
    timer = null;
  }
}
