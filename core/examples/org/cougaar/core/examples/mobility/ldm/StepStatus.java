/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
package org.cougaar.core.examples.mobility.ldm;

import java.io.Serializable;

import org.cougaar.core.mobility.ldm.MoveAgent;

/**
 * Mutable status of step processing.
 */
public final class StepStatus implements Serializable {

  /**
   * State constants.
   */
  public static final int UNSEEN  = 0;
  public static final int PAUSED  = 1;
  public static final int RUNNING = 2;
  public static final int SUCCESS = 3;
  public static final int FAILURE = 4;
  public static final int TIMEOUT = 5;

  /**
   * Dummy results for use instead of null, to indicate
   * that there are no current results.
   */
  public static final StepStatus NONE =
    new StepStatus(UNSEEN, -1, -1, null);

  // ticket id?

  private final int state;
  private final long startTime;
  private final long endTime;
  private final MoveAgent.Status moveStatus;

  public StepStatus(
      int state,
      long startTime,
      long endTime,
      MoveAgent.Status moveStatus) {
    this.state = state;
    this.startTime = startTime;
    this.endTime = endTime;
    this.moveStatus = moveStatus;
  }

  /**
   * Get the state code (RUNNING, TIMEOUT, etc).
   */
  public int getState() {
    return state;
  }

  /**
   * Get a String representation of the state code.
   */
  public String getStateAsString() {
    switch (state) {
      case UNSEEN: return "UNSEEN";
      case PAUSED: return "PAUSED";
      case RUNNING: return "RUNNING";
      case SUCCESS: return "SUCCESS";
      case FAILURE: return "FAILURE";
      case TIMEOUT: return "TIMEOUT";
      default: return "UNKNOWN("+state+")";
    }
  }

  /**
   * If the state is {RUNNING, SUCCESS, FAILURE, or TIMEOUT},
   * get the time in milliseconds when the run started, 
   * otherwise -1 is returned.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * If the state is {SUCCESS, FAILURE, or TIMEOUT}, get the 
   * time in milliseconds when the run started, otherwise -1
   * is returned.
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * If the state is {SUCCESS or FAILURE}, get the 
   * move status if available.
   */
  public MoveAgent.Status getMoveStatus() {
    return moveStatus;
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof StepStatus)) {
      return false;
    } else {
      StepStatus ss = (StepStatus) o;
      return 
        (state == ss.state) &&
        (startTime == ss.startTime) &&
        (endTime == ss.endTime) &&
        ((moveStatus != null) ?
         (moveStatus.equals(ss.moveStatus)) :
         (ss.moveStatus == null));
    }
  }
  public int hashCode() {
    return ((int) startTime);
  }
  
  public String toString() {
    return 
      "status {"+
      "\n  state:       "+getStateAsString()+
      "\n  start:       "+startTime+
      "\n  end:         "+endTime+
      "\n  move status: "+moveStatus+
      "\n}";
  }
}
