/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.node;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.agent.service.alarm.ExecutionTimer;
import org.cougaar.core.agent.service.alarm.RealTimer;
import org.cougaar.core.agent.service.alarm.Timer;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.node.service.NaturalTimeService;
import org.cougaar.core.node.service.RealTimeService;
import org.cougaar.core.service.ThreadService;

/** Pseudo-component implementing NaturalTimeService and RealTimeService ServiceProvider
 * and holding references to the actual timers.
 **/
class TimeServiceProvider 
  implements ServiceProvider 
{
  private ExecutionTimer xTimer;
  private Timer rTimer;
  private ServiceBroker sb;

  TimeServiceProvider(ServiceBroker sb) {
    this.sb = sb;
  }

  /** Starts the timers **/
  void start() {
    ThreadService tsvc = (ThreadService)
        sb.getService(this, ThreadService.class, null);
    xTimer = new ExecutionTimer();
    xTimer.start(tsvc);
    rTimer = new RealTimer();
    rTimer.start(tsvc);
    sb.releaseService(this, ThreadService.class, tsvc);
  }
    
  void stop() {
    throw new IllegalArgumentException("Not implemented");
  }

  private final Set services = new HashSet(11);

  // implement ServiceProvider
  public Object getService(ServiceBroker xsb, Object requestor, Class serviceClass) {
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
    private final String name;
    protected TSPI(Object requestor) {
      this.requestor = requestor;
      name = (this.getClass().getName())+" for "+(requestor.toString());
    }

    public String toString() { return name; }

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
      
      public String toString() {
        return "AlarmWrapper("+alarm+") of "+(TSPI.this.toString());
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
    /**
     * @deprecated Use the version that allows specifying absolute change time instead
     */ 
    public ExecutionTimer.Parameters createParameters(long millis, boolean millisIsAbsolute, double newRate,
                                                      boolean forceRunning, long changeDelay) {
      return xTimer.create(millis, millisIsAbsolute, newRate, forceRunning, changeDelay);
    }
    public ExecutionTimer.Parameters createParameters(long millis, boolean millisIsAbsolute, double newRate,
                                                      boolean forceRunning, long changeTime, boolean changeIsAbsolute) {
      return xTimer.create(millis, millisIsAbsolute, newRate, forceRunning, changeTime, changeIsAbsolute);
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


