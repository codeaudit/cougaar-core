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
import org.cougaar.core.cluster.PeriodicAlarm;
import java.util.*;

/** 
 * Instantiable Timer to track Real (System, "Wall Clock") Time.
 **/

public class RealTimer extends Timer {
  public RealTimer() {}

  protected String getName() {
    return "RealTimer";
  }

  /* /////////////////////////////////////////////////////// 

  // point test 

  public static void main(String args[]) {
    // create a timer
    Timer timer = new RealTimer();
    timer.start();

    System.err.println("currentTimeMillis() = "+timer.currentTimeMillis());
    // test running advance
    timer.addAlarm(timer.createTestAlarm(5*1000));// 5 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec
    timer.addAlarm(timer.createTestAlarm(10*1000)); // 10 sec (again)
    timer.addAlarm(timer.createTestAlarm(20*1000));
    timer.addAlarm(timer.createTestAlarm(30*1000));
    timer.addAlarm(timer.createTestAlarm(40*1000));
    timer.addAlarm(timer.createTestAlarm(60*60*1000)); // 60 min
    timer.sleep(120*1000);      // wait 10 seconds      
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
  */
}
