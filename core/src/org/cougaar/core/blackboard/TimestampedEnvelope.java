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
package org.cougaar.core.blackboard;

/**
 * An envelope that additionally notes the subscriber name,
 * time of the "openTransaction()", and time of the 
 * "closeTransaction()".
 * 
 * @see Subscriber option system property that must be enabled
 *    for these Envelopes to be used.
 */
public class TimestampedEnvelope extends Envelope {

  private String name;
  private long openTime;
  private long closeTime;

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
