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
 * Metrics on the open and close times of transactions.
 * <p>
 * @see EnvelopeMetricsSubscription
 */
public final class EnvelopeMetrics implements java.io.Serializable {

  /**
   * If "isBlackboard()" is true, then the "getName()" response
   * is == to this interned "&lt;blackboard&gt;" string constant.
   */
  public static final String BLACKBOARD = "<blackboard>";

  /**
   * If "isBlackboard()" is false, and the name is not known,
   * the then "getName()" response is == to the interned
   * "&lt;unknown&gt;" string constant.
   */
  public static final String UNKNOWN = "<unknown>";

  private final String name;
  private final long openTime;
  private final long closeTime;

  public EnvelopeMetrics(TimestampedEnvelope te) {
    this.name = _getName(te);
    this.openTime = te.getTransactionOpenTime();
    this.closeTime = te.getTransactionCloseTime();

    // could also easily get the raw tuples and 
    // count the number of adds / changes / removes
  }

  /**
   * @return true if the envelope is from the blackboard (LPs)
   */
  public final boolean isBlackboard() { return (name == null); }

  /**
   * @return the name of the subscriber that created this envelope.
   * @see #BLACKBOARD
   * @see #UNKNOWN
   */
  public final String getName() { 
    return ((name != null) ? name : BLACKBOARD);
  }

  /**
   * @return time in milliseconds when the transaction was opened
   */
  public final long getTransactionOpenTime() { return openTime; }

  /**
   * @return time in milliseconds when the transaction was closed
   */
  public final long getTransactionCloseTime() { return closeTime; }

  //
  // use the name as the hash-code and equality?
  //

  public String toString() {
    return 
      "EnvelopeMetrics {"+
      "\n  bb:    "+isBlackboard()+
      "\n  name:  "+getName()+
      "\n  open:  "+openTime+
      "\n  close: "+closeTime+" (+"+(closeTime-openTime)+")"+
      "}";
  }

  // helper for constructor
  private static final String _getName(TimestampedEnvelope te) {
    if (te.isBlackboard()) {
      return null;
    }
    String s = te.getName();
    if (s == null) {
      return UNKNOWN;
    }
    /*
    // is this worth checking?
    if (s.equals(BLACKBOARD)) {
      return "dup"+BLACKBOARD;
    } else if (s.equals(UNKNOWN)) {
      return "dup"+UNKNOWN;
    }
    */
    return s;
  }

  private static final long serialVersionUID = -5208392019823789283L;
}
