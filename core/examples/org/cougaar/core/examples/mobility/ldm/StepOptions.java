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
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;

/**
 * Immutable step configuration options.
 */
public final class StepOptions implements Serializable {

  private final Object ownerId;
  private final MessageAddress source;
  private final MessageAddress target;
  private final Ticket ticket;
  private final long pauseTime;
  private final long timeoutTime;

  public StepOptions(
      Object ownerId,
      MessageAddress source,
      MessageAddress target,
      Ticket ticket,
      long pauseTime,
      long timeoutTime) {
    this.ownerId = ownerId;
    this.source = source;
    this.target = target;
    this.ticket = ticket;
    this.pauseTime = pauseTime;
    this.timeoutTime = timeoutTime;
    if (ticket == null) {
      throw new IllegalArgumentException(
          "null ticket");
    }
    if ((pauseTime > 0) &&
        (timeoutTime > 0) &&
        (pauseTime > timeoutTime)) {
      throw new IllegalArgumentException(
          "pause time ("+pauseTime+") must be <="+
          " to the timeout time ("+timeoutTime+")");
    }
  }

  public Object getOwnerId() {
    return ownerId;
  }
  public MessageAddress getSource() {
    return source;
  }
  public MessageAddress getTarget() {
    return target;
  }
  public Ticket getTicket() {
    return ticket;
  }
  public long getPauseTime() {
    return pauseTime;
  }
  public long getTimeoutTime() {
    return timeoutTime;
  }
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof StepOptions)) {
      return false;
    } else {
      StepOptions so = (StepOptions) o;
      return 
        ((ownerId != null) ?
         (ownerId.equals(so.ownerId)) :
         (so.ownerId == null)) &&
        ((source != null) ?
         (source.equals(so.source)) :
         (so.source == null)) &&
        ((target != null) ? 
         (target.equals(so.target)) :
         (so.target == null)) &&
        ticket.equals(so.ticket) &&
        (pauseTime == so.pauseTime) &&
        (timeoutTime == so.timeoutTime);
    }
  }
  public int hashCode() {
    return 
      ((ownerId != null) ? ownerId.hashCode() : 5) ^
      ticket.hashCode() ^
      ((source != null) ? source.hashCode() : 7);
  }
  
  public String toString() {
    return 
      "step {"+
      "\n  ownerId: "+ownerId+
      "\n  source:  "+source+
      "\n  target:  "+target+
      "\n  ticket:  "+ticket+
      "\n  pause:   "+pauseTime+
      "\n  timeout: "+timeoutTime+
      "\n}";
  }
}
