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

package org.cougaar.core.agent.service.alarm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;

import org.cougaar.util.PropertyParser;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;

/** Implement a basic timer class that activates Alarm instances on or
 * after a specific time.  The base class operated on System time, but 
 * subclasses may operate on different scales.
 *
 * Visible feedback may be controlled by standard logging for class:
 * org.cougaar.core.agent.service.alarm.Timer:
 * 
 * WARN also enables logging of when (real-time only) alarms are more than Epsilon millis late 
 * INFO also enables logging of when alarms take more than Epsilon millis to ring
 * DEBUG also enables reports of every alarm ringing.
 *
 * Subclasses may override the feedback printed.
 * @property org.cougaar.core.agent.service.alarm.Timer.epsilon=10000 milliseconds
 * considered a relatively long time for alarm delivery.
 *
 **/

public abstract class Timer implements Runnable {
  protected final static Logger log = Logging.getLogger(Timer.class);

  protected static long epsilon = 10*1000L;

  static {
    epsilon = PropertyParser.getLong("org.cougaar.core.agent.service.alarm.Timer.epsilon", epsilon);
  }

  /** all alarms **/
  private ArrayList alarms = new ArrayList();
  /** Pending Periodic Alarms.  
   * PeriodicAlarms which have gone off but
   * need to be added back on.  These are collected and added
   * back in a second pass so that we don't get terrible behavior
   * if someone abuses a periodic alarm
   **/
  // only modified in the run loop
  private ArrayList ppas = new ArrayList();

  /** Pending Alarms.  
   * alarms which need to be rung, but we haven't gotten around to yet.
   **/
  // only modified in the run loop thread
  private ArrayList pas = new ArrayList();


  private static final Comparator comparator = new Comparator() {
      public int compare(Object a, Object b) {
        long ta = ((Alarm)a).getExpirationTime();
        long tb = ((Alarm)b).getExpirationTime();
        if (ta>tb) return 1;
        if (ta==tb) return 0;
        return -1;
      }};

  protected Object sem = new Object();

  /** must be called only within a sync(sem) **/
  private void insert(Alarm alarm) {
    ListIterator i = alarms.listIterator(0);
    
    if (! i.hasNext()) {        // no elements?
      alarms.add(alarm);
    } else {
      // find the right insertion point
      while (i.hasNext()) {
        Alarm cur = (Alarm) i.next();
        // stop if the alarm is < the current insertion point
        if (comparator.compare(alarm, cur) < 0) {
          i.previous();         // back up one step
          i.add(alarm);         // add before cur
          return;               
        }
      }
      // no elements were greater, add at end
      i.add(alarm);
    }
  }

  public String alarmsToString() {
    synchronized (sem) {
      String s = "[";
      Iterator i = alarms.iterator();
      while(i.hasNext()) {
        s = s+(i.next());
        if (i.hasNext()) s=s+", ";
      }
      s=s+"]";
      return s;
    }
  }
  public void addAlarm(Alarm alarm) {
    synchronized (sem) {
      insert(alarm);
      //System.err.println("Alarms = "+alarmsToString()); // debug
      sem.notify();
    }
    Thread.yield();
  }

  public void cancelAlarm(Alarm alarm) {
    synchronized (sem) {
      alarms.remove(alarm);
      sem.notify();
    }
    Thread.yield();
  }

  // must be called within sync(sem) 
  private Alarm peekAlarm() {
    if (alarms.isEmpty())
      return null;
    else
      return (Alarm) alarms.get(0);
  }
  // must be called within sync(sem) 
  private Alarm nextAlarm() {
    if (alarms.isEmpty()) return null;
    Alarm top = (Alarm) alarms.get(0);
    if (top != null) 
      alarms.remove(0);
    if (alarms.isEmpty()) return null;
    return (Alarm) alarms.get(0);
  }

  public void run() {
    while (true) {
      long time;
      synchronized (sem) {
        Alarm top = peekAlarm();
          
        // wait block
        try {
          if (top == null) {    // no pending events?
            sem.wait();         //   ... just wait forever
          } else {              // otherwise, figure out how long to wait
            long delta = top.getExpirationTime() - currentTimeMillis();
            double rate = getRate();
            long maxWait = getMaxWait();
            if (rate > 0.0) {
              delta = Math.min((long) (delta / rate), maxWait);
            } else {            // Time is standing still
              delta = maxWait;  // Wait until next significant change in timer
            }
            if (delta > 0) {
              if (delta < 100) delta=100; // min of .1 second wait time
              sem.wait(delta);
            }
          }
        } catch (InterruptedException ie) {
          //System.err.println("Interrupted "+ie);
          // don't care, just continue
        }

        // fire some alarms
        top = peekAlarm();
        time = currentTimeMillis();
        while ( top != null && 
                time >= top.getExpirationTime() ) {
          pas.add(top);
          top = nextAlarm();
        }

      } // sync(sem)
      
      // now ring any outstanding alarms: outside the sync
      // just in case an alarm ringer tries setting another alarm!
      {
        int l = pas.size();
        for (int i = 0; i<l; i++) {
          Alarm top = (Alarm) pas.get(i);
          try {
            ring(top);
          } catch (Throwable e) {
            System.err.println("Alarm "+top+" generated Exception: "+e);
            e.printStackTrace();
            // cancel error generating alarms to be certain.
            top.cancel();
          }
          
          // handle periodic alarms
          if (top instanceof PeriodicAlarm) {
            ppas.add(top);      // consider adding it back later
          }
        }
        pas.clear();
      }

      // back in sync, reset any periodic alarms
      synchronized (sem) {
        // reset periodic alarms
        int l = ppas.size();
        for (int i=0; i<l; i++) {
          PeriodicAlarm ps = (PeriodicAlarm) ppas.get(i);
          ps.reset(time);       // reset it
          if (!ps.hasExpired()) { // if it hasn't expired, add it back to the queue
            insert(ps);
          }
        }
        ppas.clear();
      } // sync(sem)
    } // infinite loop
  }

  private void ring(Alarm alarm) {
    if (!alarm.hasExpired()) {  // only ring if it wasn't cancelled already
      report(alarm);
      long dt = 0L;
      try {
        dt = System.currentTimeMillis(); // real start time
        alarm.expire();
        dt = System.currentTimeMillis() - dt; // real delta time
        //
        if (dt > epsilon) {
          if (log.isWarnEnabled()) {
            log.warn("Alarm "+alarm+" blocked for "+dt+"ms while ringing");
          }
        }
      } finally {
        // see if the alarm has been evil and as has opened a transaction
        // but neglected to close it
        if (org.cougaar.core.blackboard.Subscriber.abortTransaction()) {
          log.error("Alarm "+alarm+" failed to close it's transaction");
        }
      }
    }
  }

  protected void report(Alarm alarm) {
    if (log.isDebugEnabled()) {
      log.debug("Ringing "+alarm);
    }
  }

  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  protected double getRate() {
    return 1.0;
  }

  /**
   * Override this to specify time before next rate change. It is
   * always safe to underestimate.
   **/
  protected long getMaxWait() {
    return 10000000000L;        // A long time
  }

  public Timer() {}

  private Thread timerThread = null;

  public void start() {
    timerThread = new Thread(this, getName());
    timerThread.start();
  }

  protected String getName() {
    return "Timer";
  }

}
