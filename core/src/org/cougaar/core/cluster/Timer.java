/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.Alarm;
import org.cougaar.core.cluster.PeriodicAlarm;
import java.util.*;

/** Implement a basic timer class that activates Alarm instances on or
 * after a specific time.  The base class operated on System time, but 
 * subclasses may operate on different scales.
 *
 * Visible feedback may be controlled by the system property
 * "org.cougaar.timer"  Valid values are:
 *   "quiet"	no feedback.
 *   "visible"	an asterix is printed to err on each timer expiration.
 *   "verbose"  a line with the timer and the alarm is printed on each expiration.
 * The default is "org.cougaar.timer=quiet".
 * Subclasses may override the feedback printed.
 * @property org.cougaar.timer Controls visibility of timer expirations.  See class description for 
 * more information.
 **/

public abstract class Timer implements Runnable {
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

  protected static boolean isVisible = false;
  protected static boolean isLoud = false;
  static {
    String verbosity = System.getProperty("org.cougaar.timer", "quiet");
    if (verbosity.equals("quiet")) {
      isVisible=false;
      isLoud=false;
    } else if (verbosity.equals("visible")) {
      isVisible=true;
      isLoud=false;
    } else if (verbosity.equals("verbose")) {
      isVisible=true;
      isLoud=true;
    } else {
      System.err.println("Unknown value for org.cougaar.timer ("+verbosity+")");
    }
  }

  private static final Comparator comparator = new Comparator(){
      public int compare(Object a, Object b) {
        return (int) (((Alarm)a).getExpirationTime()-
                      ((Alarm)b).getExpirationTime());
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
          System.err.println("Interrupted "+ie);
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
      if (isVisible) report(alarm);
      alarm.expire();
    }
  }

  protected void report(Alarm alarm) {
    if (isLoud) {
      System.err.println(this.toString()+" ringing "+alarm);
    } else {
      System.err.print("*");
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
