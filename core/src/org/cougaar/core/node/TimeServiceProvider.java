/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import org.cougaar.core.agent.Agent;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.AlarmServiceProvider;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.agent.service.alarm.ExecutionTimer.Change;
import org.cougaar.core.agent.service.alarm.ExecutionTimer.Parameters;
import org.cougaar.core.agent.service.alarm.RealTimer;
import org.cougaar.core.agent.service.alarm.Timer;

import org.cougaar.core.component.*;

import org.cougaar.core.node.service.*;
import java.util.*;

/** Pseudo-component implementing NaturalTimeService and RealTimeService ServiceProvider
 * and holding references to the actual timers.
 **/
class TimeServiceProvider 
  implements ServiceProvider 
{
  private ExecutionTimer xTimer;
  private Timer rTimer;

  TimeServiceProvider() {
  }

  /** Starts the timers **/
  void start() {
    xTimer = new ExecutionTimer();
    xTimer.start();
    rTimer = new RealTimer();
    rTimer.start();
  }
    
  void stop() {
    throw new IllegalArgumentException("Not implemented");
  }

  private final Set services = new HashSet(11);

  // implement ServiceProvider
  public Object getService(ServiceBroker xsb, Object requestor, Class serviceClass) {
    if (requestor instanceof Agent) {
      if (serviceClass == NaturalTimeService.class) {
        Object s = new NaturalTimeServiceImpl(requestor);
        synchronized (services) { 
          services.add(s);
        }
        return s;
      } else if (serviceClass == RealTimeService.class) {
        Object s = new RealTimeServiceImpl(requestor);
        synchronized (services) { 
          services.add(s);
        }
        return s;
      } else {
        throw new IllegalArgumentException("Can only provide NaturalTimeService and RealTimeService!");
      }
    } else {
      throw new IllegalArgumentException("Only Agents may request NaturalTimeService and RealTimeService!");
    }
  }

  public void releaseService(ServiceBroker xsb, Object requestor, Class serviceClass, Object service) {
    synchronized (services) { 
      if (services.remove(service)) {
        ((TSPI) service).clear();
      } else {
        throw new IllegalArgumentException("Cannot release service "+service);
      }
    }
  }

  private abstract class TSPI {
    private Object requestor;
    protected TSPI(Object requestor) {
      this.requestor = requestor;
    }
    /** @return the client **/
    private Object getRequestor() {
      return requestor;
    }

    protected abstract Timer getTimer();

    public long currentTimeMillis() { return getTimer().currentTimeMillis(); }
    public void addAlarm(Alarm alarm) { getTimer().addAlarm(wrap(alarm)); }
    public void cancelAlarm(Alarm alarm) {
      Alarm w = find(alarm);
      if (w != null) {
        getTimer().cancelAlarm(w);
      }
    }

    /** clear out any saved state, e.g. remove outstanding alarms **/
    private void clear() {
      synchronized (alarms) {
        for (Iterator it = alarms.values().iterator(); it.hasNext(); ) {
          Alarm w = (Alarm) it.next();
          // should the Alarms themselves be cancelled?  I'm guessing not
          getTimer().cancelAlarm(w);
        }
      }
    }

    /** map of <Alarm,AlarmWrapper> **/
    private final Map alarms = new HashMap(11);

    /** create an AlarmWrapper around an Alarm, and remember it **/
    protected Alarm wrap(Alarm a) {
      Alarm w = new AlarmWrapper(a);
      synchronized (alarms) {
        alarms.put(a,w);
      }
      return w;
    }

    /** drop an Alarm (not an AlarmWrapper) from the remembered alarms **/
    protected void forget(Alarm a) {
      synchronized (alarms) {
        alarms.remove(a);
      }
    }

    /** Find the remembered AlarmWrapper matching a given Alarm **/
    protected AlarmWrapper find(Alarm a) {
      synchronized (alarms) {
        return (AlarmWrapper) alarms.get(a);
      }
    }

    class AlarmWrapper implements Alarm {
      private Alarm alarm;
      AlarmWrapper(Alarm alarm) {
        this.alarm = alarm;
      }
      
      public long getExpirationTime() { return alarm.getExpirationTime(); }
      public boolean hasExpired() { return alarm.hasExpired(); }

      // called by Timer to notify that the alarm has rung
      public void expire() {
        forget(alarm);
        alarm.expire();
      }

      // called by client to notify that the alarm should be cancelled
      // usually just sets hasExpired
      public boolean cancel() {
        forget(alarm);
        return alarm.cancel();
      }
    }
  }


  private class NaturalTimeServiceImpl 
    extends TSPI
    implements NaturalTimeService 
  {
    private NaturalTimeServiceImpl(Object r) {
      super(r);
    }

    protected Timer getTimer() { return xTimer; }
    public void setParameters(ExecutionTimer.Parameters x) { xTimer.setParameters(x); }
    public ExecutionTimer.Parameters createParameters(long millis, boolean millisIsAbsolute, double newRate,
                                                      boolean forceRunning, long changeDelay) {
      return xTimer.create(millis, millisIsAbsolute, newRate, forceRunning, changeDelay);
    }
    public ExecutionTimer.Parameters[] createParameters(ExecutionTimer.Change[] changes) {
      return xTimer.create(changes);
    }
    public double getRate() { return xTimer.getRate(); }
  }


  private class RealTimeServiceImpl 
    extends TSPI
    implements RealTimeService 
  {
    private RealTimeServiceImpl(Object r) {
      super(r);
    }

    protected Timer getTimer() { return rTimer; }
  }
}


