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

package org.cougaar.core.blackboard;


/**
 * An envelope that records the subscriber name, time of
 * "openTransaction()", and time of "closeTransaction()".
 * 
 * @see Subscriber option system property that must be enabled
 *    for these Envelopes to be used.
 */
public class TimestampedEnvelope extends Envelope {

  private String name;
  private long openTime;
  private long closeTime;

  public TimestampedEnvelope() {
  }

  public Envelope newInstance() {
    TimestampedEnvelope ret = new TimestampedEnvelope();
    ret.name = name;
    ret.openTime = openTime;
    ret.closeTime = closeTime;
    return ret;
  }

  public final void setName(String name) { 
    this.name = name; 
  }

  public final void setTransactionOpenTime(long openTime) { 
    this.openTime = openTime; 
  }

  public final void setTransactionCloseTime(long closeTime) { 
    this.closeTime = closeTime; 
  }

  /**
   * @return true if the envelope is from the blackboard (LPs)
   */
  public boolean isBlackboard() { return false; }

  /**
   * @return the name of the subscriber that created this envelope
   */
  public final String getName() { return name; }

  /**
   * @return time in milliseconds when the transaction was opened
   */
  public final long getTransactionOpenTime() { return openTime; }

  /**
   * @return time in milliseconds when the transaction was closed
   */
  public final long getTransactionCloseTime() { return closeTime; }

  public String toString() {
    return 
      super.toString()+
      " ("+
      (isBlackboard() ? "blackboard, " : "client, ")+
      name+", "+
      openTime+" + "+
      (closeTime-openTime)+")";
  }
}
