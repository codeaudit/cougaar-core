/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.Alarm;
import java.util.*;
import java.text.*;

/**
 * Control the advancement of Execution time. Execution time is a
 * monotonically increasing value whose rate can be controlled through
 * the API implemented in this class. Execution time is nominally the
 * same everywhere in the society. Small descrepancies may exist when
 * the parameters of the exeuction time advancement are altered.
 * Execution time is represented by a rate of advancement and an
 * offset. The rate of advancment may be zero (time stands still) or
 * any positive value. Excessively high rates may lead to anomalous
 * behavior.
 *
 * The equation of time is Te = Ts * Km + Ko where:
 *  Te is Execution time
 *  Ts is system time (System.currentTimeMillis())
 *  Km is the (positive) rate of advancement
 *  Ko is the offset
 *
 * The System.currentTimeMillis() is presumed to be in sync in all
 * clusters within a few milliseconds by using NTP (Network Time
 * Protocol). The maximum offset of the system clocks limits the
 * maximum value that Km can have without introducing serious
 * anomalies.
 *
 * It is necessary for execution time to be monotonic. Monotonicity
 * must be achieved even in the face of delays in propagating changes
 * in the parameters of execution time advancement. The message that
 * is used to alter the execution time advancement parameters
 * specifically allows for the change to occur at some future time and
 * it is expected that that will be the norm, but if the message
 * transmission is delayed or if insufficient time is allowed, the
 * equation of time must be altered to assure monotonicity.
 * Furthermore, a succession of time advancement must ultimately
 * result in all clusters using the same time parameters. This is
 * achieved by defining a sorting order on the time parameters that
 * establishes a dominance relation between all such parameter sets.
 *
 * Time change messages contain:
 *
 *   New rate
 *   New offset
 *   Changeover (real) time
 *
 * The changeover is redundant when the new rate is > 0.0 since it can
 * be computed from the current parameters and the new rate and
 * offset, but having it assists in sorting out a succession of time
 * changes from multiple origins. In particular, the changeover time
 * is the first order sorting factor. If the changeover times are
 * equal, the parameters yielding the highest execution time value at
 * the changeover time dominate. If the execution times at the
 * changeover time are equal, the change with the highest offset
 * dominates.
 **/
public class ExecutionTimer extends Timer {
  public static final long DEFAULT_CHANGE_DELAY = 10000L;

  public static class Parameters implements Comparable, java.io.Serializable {
    double theRate;             // The advancement rate (Km)
    long theOffset;             // The offset (Ko)
    long theChangeTime;         // The changeover time

    public Parameters(double aRate, long anOffset, long aChangeTime) {
      theRate = aRate;
      theOffset = anOffset;
      theChangeTime = aChangeTime;
    }

    public long computeTime(long now) {
      return (long) (now * theRate) + theOffset;
    }

    public int compareTo(Object o) {
      Parameters other = (Parameters) o;
      long diff = this.theChangeTime - other.theChangeTime;
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      diff = this.computeTime(this.theChangeTime) - other.computeTime(other.theChangeTime);
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      diff = this.theOffset - other.theOffset;
      if (diff < 0L) return -1;
      if (diff > 0L) return 1;
      return 0;
    }

    public String toString() {
      return ("Time = "
              + new Date(computeTime(theChangeTime)).toString()
              + "*"
              + theRate
              + "@"
              + new Date(theChangeTime).toString());
    }
  }

  /**
   * Description of a Parameters change (relative to some implicit Parameters)
   **/
  public static class Change {
    long theOffsetDelta;       // Step in the offset
    double theRate;             // New rate is absolute
    long theChangeTimeDelta; // Change time relative to some other Parameters
    public Change(double aRate, long anOffsetDelta, long aChangeTimeDelta) {
      theOffsetDelta = anOffsetDelta;
      theRate = aRate;
      theChangeTimeDelta = aChangeTimeDelta;
    }
  }

  /**
   * An array of Parameters. The array is sorted in the order in which
   * they are to be applied. The 0-th element is the current setting.
   **/
  Parameters[] theParameters = new Parameters[] {
    new Parameters(1.0, 0L, 0L),
    null,                       // Allow up to five new parameters
    null,
    null,
    null,
  };
  private int theParameterCount = 1;
  
  long theCurrentExecutionTime;    // This assures monotonicity

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter.
   **/
  public Parameters create(long millis,
                                        boolean millisIsAbsolute,
                                        double newRate)
  {
    synchronized (sem) {
      return create(millis, millisIsAbsolute, newRate, false, DEFAULT_CHANGE_DELAY);
    }
  }

  /**
   * Create Parameters that jump time by a specified amount and
   * continue at a new rate thereafter. The new rate is 0.0 if running
   * is false, the current rate if running is true and the current
   * rate is greater than 0.0 or 1.0 if running is true and the
   * current rate is stopped.
   **/
  public Parameters create(long millis,
                           boolean millisIsAbsolute,
                           double newRate,
                           boolean forceRunning)
  {
    synchronized (sem) {
      return create(millis, millisIsAbsolute, newRate, forceRunning, DEFAULT_CHANGE_DELAY);
    }
  }

  public Parameters create(long millis,
                           boolean millisIsAbsolute,
                           double newRate,
                           boolean forceRunning,
                           long changeDelay)
  {
    synchronized (sem) {
      long changeTime = getNow() + changeDelay;
      return create(millis, millisIsAbsolute, newRate, forceRunning, changeTime, theParameters[0]);
    }
  }

  private Parameters create(long millis, boolean millisIsAbsolute,
                            double newRate, boolean forceRunning,
                            long changeTime,
                            Parameters relativeTo)
  {
    long valueAtChangeTime = relativeTo.computeTime(changeTime);
    if (Double.isNaN(newRate)) newRate = relativeTo.theRate;
    if (Double.isInfinite(newRate)) {
      throw new IllegalArgumentException("Illegal infinite rate");
    }
    if (newRate < 0.0) {
      throw new IllegalArgumentException("Illegal negative rate: " + newRate);
    }
    if (forceRunning) {
      if (newRate == 0.0) {
        newRate = relativeTo.theRate;
      }
      if (newRate == 0.0) {
        newRate = 1.0;
      }
    }
    if (millisIsAbsolute) {
      millis = millis - valueAtChangeTime;
    }
    if (millis < 0L) {
      throw new IllegalArgumentException("Illegal negative advancement:" + millis);
    }
    long newOffset = valueAtChangeTime + millis - (long) (changeTime * newRate);
    return new Parameters(newRate, newOffset, changeTime);
  }

  /**
   * Creates a series of changes. The first change is relative to the
   * current Parameters, the subsequent changes are relative to the
   * previous change.
   **/
  public Parameters[] create(Change[] changes) {
    synchronized (sem) {
      Parameters[] result = new Parameters[changes.length];
      Parameters prev = theParameters[0];
      long changeTime = getNow();
      for (int i = 0; i < changes.length; i++) {
        Change change = changes[i];
        if (change.theChangeTimeDelta <= 0.0) {
          throw new IllegalArgumentException("Illegal non-positive change time delta: "
                                             + change.theChangeTimeDelta);
        }
        changeTime += change.theChangeTimeDelta;
        result[i] = create(change.theOffsetDelta, false,
                           change.theRate, false,
                           changeTime, prev);
        prev = result[i];
      }
      return result;
    }
  }

  /**
   * Get the current real time and insure that the current parameters
   * are compatible with the real time being returned. The pending
   * parameters become current if their time of applicability has been
   * reached.
   **/
  private long getNow() {
    long now = System.currentTimeMillis();
    while (theParameterCount > 1 && theParameters[1].theChangeTime <= now) {
      System.arraycopy(theParameters, 1, theParameters, 0, --theParameterCount);
      theParameters[theParameterCount] = null;
    }
    return now;
  }

  /**
   * Get the current execution time in millis.
   **/
  public long currentTimeMillis() {
    synchronized (sem) {
      long newTime = theParameters[0].computeTime(getNow());
      if (newTime > theCurrentExecutionTime) {
        theCurrentExecutionTime = newTime; // Only advance time, never decreases
      }
      return theCurrentExecutionTime;
    }
  }

  /**
   * Insert new Parameters into theParameters. If the new parameters
   * apply before the last element of theParameters, ignore them.
   * Otherwise, append the new parameters overwriting the last parameters if necessary.
   **/
  public void setParameters(Parameters parameters) {
    synchronized (sem) {
      getNow();                   // Bring parameters up-to-now
      if (parameters.compareTo(theParameters[theParameterCount - 1]) > 0) {
        if (theParameterCount < theParameters.length) {
          System.out.println("Setting parameters " + theParameterCount + " to " + parameters);
          theParameters[theParameterCount++] = parameters;
        } else {
          System.out.println("Setting parameters " + (theParameterCount-1) + " to " + parameters);
          theParameters[theParameterCount - 1] = parameters;
        }
      }
      sem.notify();
    }
  }

  protected long getMaxWait() {
    if (theParameterCount > 1) {
      return theParameters[1].theChangeTime - System.currentTimeMillis();
    } else {
      return 100000000L;
    }
  }

  public double getRate() {
    synchronized (sem) {
      getNow();                   // Bring parameters up-to-now
      return theParameters[0].theRate;
    }
  }

  /**
   * Initialize execution time to a particular time. This is
   * problematic since we represent execution time as an offset from
   * system time. Computing that offset consistently across all
   * clusters means that we have to compute a time (in the past) at
   * which the parameters became effective and then compute the offset
   * relative to that. There is no way for all clusters to reliably
   * compute the same value for this time. The best we can do is
   * select a boundary that is unlikely to be near the actual starting
   * time and compute relative to that.
   **/
  public ExecutionTimer() {
    String startTime = System.getProperty("org.cougaar.core.cluster.startTime");
    if (startTime != null) {
      try {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(Calendar.HOUR_OF_DAY, 0); // Midnight today
        long offset = (new SimpleDateFormat("MM/dd/yyy")).parse(startTime).getTime() - calendar.getTime().getTime();
        theParameters[0] = new Parameters(1.0, offset, 0L);
      } catch (Exception e) {
        System.err.println("Bad org.cougaar.core.cluster.startTime: " + e);
      }
    }
  }

  protected String getName() {
    return "ExecutionTimer";
  }

  /* /////////////////////////////////////////////////////// 

  // point test 

  public static void main(String args[]) {
    // create a timer
    ExecutionTimer timer = new ExecutionTimer();
    timer.start();

    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    // test running advance
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000+30*1000)); // 60min+30sec
    timer.addAlarm(timer.createTestAlarm(30*60*1000)); // 30 min
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec (again)
    timer.sleep(15*1000);      // wait 10 seconds      
    System.err.println("advancing running time 60 minutes");
    timer.advanceRunningOffset(60*60*1000);
    timer.sleep(20*1000);      // wait 20sec
    System.err.println("done waiting for running time.");

    // stopped tests
    System.err.println("Trying stopped tests:");
    long t = timer.currentTimeMillis()+10*1000;
    timer.advanceStoppedTime(t);
    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(30*60*1000)); // 30 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.addAlarm(timer.createTestAlarm(60*60*1000+30*1000)); // 60min+30sec
    timer.sleep(15*1000);      // wait 10 seconds      
    System.err.println("advancing stopped time 5 seconds");
    timer.advanceStoppedTime(t+5*1000);
    timer.sleep(1*1000);      // sleep a second
    System.err.println("advancing stopped time to 10 seconds");
    timer.advanceStoppedTime(t+10*1000);
    timer.sleep(1*1000);      // sleep a second
    System.err.println("advancing stopped time to 60 minutes");
    timer.advanceStoppedTime(t+60*60*1000);
    timer.sleep(1*1000);      // wait 20sec
    System.err.println("starting clock");
    timer.startRunning();
    timer.sleep(20*1000);      // wait 20sec
    System.err.println("done waiting for running time.");


    System.exit(0);
  }

  public void sleep(long millis) {
    try {
      synchronized(this) {
        this.wait(millis);
      }
    } catch (InterruptedException ie) {}
  }
    

  Alarm createTestAlarm(long delta) {
    return new TestAlarm(delta);
  }
  private class TestAlarm implements Alarm {
    long exp;
    public TestAlarm(long delta) { this.exp = currentTimeMillis()+delta; }
    public long getExpirationTime() {return exp;}
    public void expire() { System.err.println("Alarm "+exp+" expired.");}
    public String toString() { return "<"+exp+">";}
    public boolean cancel() {}  // doesn't support cancel
  }

  /* */
}
