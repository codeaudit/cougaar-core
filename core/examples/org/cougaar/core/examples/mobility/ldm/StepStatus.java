/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
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
