/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.alarm;

/** 
 * Instantiable Timer to track Real (System, "Wall Clock") Time.
 **/

public class RealTimer extends Timer {
  public RealTimer() {}

  protected String getName() {
    return "RealTimer";
  }

  protected void report(Alarm alarm) {
    long now = currentTimeMillis();
    long at = alarm.getExpirationTime();
    if ((at+epsilon)<now) {
      // if we're more then epsilon late, we'll warn
      if (log.isInfoEnabled()) {
        log.info("Alarm "+alarm+" is "+(now-at)+"ms late");
      }
    }
    super.report(alarm);
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
