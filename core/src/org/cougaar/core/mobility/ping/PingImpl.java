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

package org.cougaar.core.mobility.ping;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;

/**
 * Package-private implementation of a Ping.
 * <p>
 * This uses the Relay support to transfer the data
 * between the source agent and target agent.
 */
final class PingImpl 
implements Ping, Relay.Source, Relay.Target, Serializable {

  public static final String PROP_START_MILLLIS = "startMillis";
  public static final String PROP_DELAY_MILLIS = "delayMillis";
  public static final String PROP_TIMEOUT_MILLIS = "timeoutMillis";
  public static final String PROP_EVENT_MILLIS = "eventMillis";
  public static final String PROP_EVENT_COUNT = "eventCount";
  public static final String PROP_LIMIT = "limit";
  public static final String PROP_IGNORE_ROLLBACK = "ignoreRollback";
  public static final String PROP_SEND_FILLER_SIZE = "sendFillerSize";
  public static final String PROP_SEND_FILLER_RAND = "sendFillerRand";
  public static final String PROP_ECHO_FILLER_SIZE = "echoFillerSize";
  public static final String PROP_ECHO_FILLER_RAND = "echoFillerRand";

  public static final long DEFAULT_START_MILLIS = 0;
  public static final long DEFAULT_DELAY_MILLIS = 2000;
  public static final long DEFAULT_TIMEOUT_MILLIS = -1;
  public static final long DEFAULT_EVENT_MILLIS = -1;
  public static final int DEFAULT_EVENT_COUNT = -1;
  public static final int DEFAULT_LIMIT = -1;
  public static final boolean DEFAULT_IGNORE_ROLLBACK = false;
  public static final int DEFAULT_SEND_FILLER_SIZE = -1;
  public static final boolean DEFAULT_SEND_FILLER_RAND = true;
  public static final int DEFAULT_ECHO_FILLER_SIZE = -1;
  public static final boolean DEFAULT_ECHO_FILLER_RAND = true;

  private static final Random rand = new Random();

  private final UID uid;
  private final MessageAddress source;
  private final MessageAddress target;
  private final long delayMillis;
  private final long timeoutMillis;
  private final long eventMillis;
  private final int eventCount;
  private final boolean ignoreRollback;
  private final int limit;
  private final int sendFillerSize;
  private final boolean sendFillerRand;
  private final int echoFillerSize;
  private final boolean echoFillerRand;

  private long sendTime;
  private long replyTime;
  private int sendCount;
  private int echoCount;
  private int replyCount;
  private String error;

  private long eventTime;

  private int statCount;
  private long statMinRTT;
  private long statMaxRTT;
  private long statSumRTT;
  private long statSumSqrRTT;

  private transient Set _targets;
  private transient Relay.TargetFactory _factory;

  public PingImpl(
      UID uid,
      MessageAddress source,
      MessageAddress target,
      Map props) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    if ((uid == null) ||
        (source == null) ||
        (target == null)) {
      throw new IllegalArgumentException(
          "null uid/source/target");
    }
    if (source.equals(target)) {
      throw new IllegalArgumentException(
          "Source and target addresses are equal ("+
          uid+", "+source+", "+target+")");
    }
    if (props == null) {
      props = Collections.EMPTY_MAP;
    }

    // parse options
    String s;
    s = (String) props.get(PROP_DELAY_MILLIS);
    delayMillis = 
      (s == null ? DEFAULT_DELAY_MILLIS : Long.parseLong(s));

    s = (String) props.get(PROP_TIMEOUT_MILLIS);
    timeoutMillis = 
      (s == null ? DEFAULT_TIMEOUT_MILLIS : Long.parseLong(s));

    s = (String) props.get(PROP_EVENT_MILLIS);
    eventMillis = 
      (s == null ? DEFAULT_EVENT_MILLIS : Long.parseLong(s));

    s = (String) props.get(PROP_EVENT_COUNT);
    eventCount = 
      (s == null ? DEFAULT_EVENT_COUNT : Integer.parseInt(s));

    s = (String) props.get(PROP_LIMIT);
    limit = 
      (s == null ? DEFAULT_LIMIT : Integer.parseInt(s));

    s = (String) props.get(PROP_IGNORE_ROLLBACK);
    ignoreRollback = 
      (s == null ? DEFAULT_IGNORE_ROLLBACK :
       "true".equalsIgnoreCase(s));

    s = (String) props.get(PROP_SEND_FILLER_SIZE);
    sendFillerSize = 
      (s == null ? DEFAULT_SEND_FILLER_SIZE :
       Integer.parseInt(s));

    s = (String) props.get(PROP_SEND_FILLER_RAND);
    sendFillerRand = 
      (s == null ? DEFAULT_SEND_FILLER_RAND :
       "true".equalsIgnoreCase(s));

    s = (String) props.get(PROP_ECHO_FILLER_SIZE);
    echoFillerSize = 
      (s == null ? DEFAULT_ECHO_FILLER_SIZE :
       Integer.parseInt(s));

    s = (String) props.get(PROP_ECHO_FILLER_RAND);
    echoFillerRand = 
      (s == null ? DEFAULT_ECHO_FILLER_RAND :
       "true".equalsIgnoreCase(s));

    // initialize
    sendTime = System.currentTimeMillis();
    replyTime = -1;
    sendCount = 0;
    echoCount = 0;
    replyCount = 0;
    error = null;
    eventTime = 0;
    statCount = 0;
    statMinRTT = 0;
    statMaxRTT = 0;
    statSumRTT = 0;
    statSumSqrRTT = 0;
    cacheTargets();
  }

  public PingImpl(
      UID uid, 
      MessageAddress source,
      MessageAddress target,
      long delayMillis,
      long timeoutMillis,
      long eventMillis,
      int eventCount,
      boolean ignoreRollback,
      int limit,
      int sendFillerSize,
      boolean sendFillerRand,
      int echoFillerSize,
      boolean echoFillerRand) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.delayMillis = delayMillis;
    this.timeoutMillis = timeoutMillis;
    this.eventMillis = eventMillis;
    this.eventCount = eventCount;
    this.ignoreRollback = ignoreRollback;
    this.limit = limit;
    this.sendFillerSize = sendFillerSize;
    this.sendFillerRand = sendFillerRand;
    this.echoFillerSize = echoFillerSize;
    this.echoFillerRand = echoFillerRand;
    if ((uid == null) ||
        (source == null) ||
        (target == null)) {
      throw new IllegalArgumentException(
          "null uid/source/target");
    }
    if (source.equals(target)) {
      throw new IllegalArgumentException(
          "Source and target addresses are equal ("+
          uid+", "+source+", "+target+")");
    }
    sendTime = System.currentTimeMillis();
    replyTime = -1;
    sendCount = 0;
    echoCount = 0;
    replyCount = 0;
    error = null;
    eventTime = 0;
    statCount = 0;
    statMinRTT = 0;
    statMaxRTT = 0;
    statSumRTT = 0;
    statSumSqrRTT = 0;
    cacheTargets();
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  // Ping:

  public MessageAddress getSource() {
    return source;
  }
  public MessageAddress getTarget() {
    return target;
  }
  public long getDelayMillis() {
    return delayMillis;
  }
  public long getTimeoutMillis() {
    return timeoutMillis;
  }
  public long getEventMillis() {
    return eventMillis;
  }
  public int getEventCount() {
    return eventCount;
  }
  public boolean isIgnoreRollback() {
    return ignoreRollback;
  }
  public int getLimit() {
    return limit;
  }
  public int getSendFillerSize() {
    return sendFillerSize;
  }
  public boolean isSendFillerRandomized() {
    return sendFillerRand;
  }
  public int getEchoFillerSize() {
    return echoFillerSize;
  }
  public boolean isEchoFillerRandomized() {
    return echoFillerRand;
  }
  public long getSendTime() {
    return sendTime;
  }
  public long getReplyTime() {
    return replyTime;
  }
  public int getSendCount() {
    return sendCount;
  }
  public int getEchoCount() {
    return echoCount;
  }
  public int getReplyCount() {
    return replyCount;
  }
  public String getError() {
    return error;
  }
  public long getEventTime() {
    return eventTime;
  }
  public void setEventTime(long now) {
    eventTime = now;
  }
  public int getStatCount() {
    return statCount;
  }
  public long getStatMinRTT() {
    return statMinRTT;
  }
  public long getStatMaxRTT() {
    return statMaxRTT;
  }
  public double getStatMeanRTT() {
    int sc = statCount;
    if (sc <= 0) {
      return 0.0;
    }
    return (statSumRTT / sc);
  }
  public double getStatSumSqrRTT() {
    return statSumSqrRTT;
  }
  public double getStatStdDevRTT() {
    int sc = statCount;
    if (sc <= 0) {
      return 0.0;
    }
    double d;
    d = statSumSqrRTT * statCount;
    d -= (statSumRTT * statSumRTT);
    d = Math.sqrt(d);
    d /= sc;
    return d;
  }
  public void setError(String error) {
    if (this.error != null) {
      throw new RuntimeException(
          "Error message already set to "+this.error);
    }
    this.error = error;
  }
  public void recycle() {
    // update statistics
    long rtt = replyTime - sendTime;
    if (statCount++ == 0) {
      statMinRTT = rtt;
      statMaxRTT = rtt;
    } else if (rtt < statMinRTT) {
      statMinRTT = rtt;
    } else if (rtt > statMaxRTT) {
      statMaxRTT = rtt;
    }
    statSumRTT += rtt;
    statSumSqrRTT += (rtt*rtt);
    // advance to the next ping
    sendTime = System.currentTimeMillis();
    replyTime = -1;
    sendCount++;
    // caller must publish-change!
  }
  public void resetStats() {
    statCount = 0;
    statMinRTT = 0;
    statMaxRTT = 0;
    statSumRTT = 0;
    statSumSqrRTT = 0;
  }

  // Relay.Source:

  private void cacheTargets() {
    _targets = Collections.singleton(target);
    _factory = new PingImplFactory(target);
  }
  public Set getTargets() {
    return _targets;
  }
  public Object getContent() {
    return new PingData(
        ignoreRollback, echoFillerSize, echoFillerRand,
        sendCount, sendFillerSize, sendFillerRand);
  }
  public Relay.TargetFactory getTargetFactory() {
    return _factory;
  }
  public int updateResponse(
      MessageAddress target, Object response) {
    // assert targetAgent.equals(target)
    // assert response != null
    if (error != null) {
      // prior error.
      return Relay.NO_CHANGE;
    }
    if ((limit > 0) &&
        (sendCount >= limit)) {
      // at limit
      return Relay.NO_CHANGE;
    }
    if (response instanceof String) {
      // new target-side error
      error = (String) response;
      return Relay.RESPONSE_CHANGE;
    }
    PingData pingData = (PingData) response;
    int newEchoCount = pingData.getCount();
    // check new echo count
    if (ignoreRollback) {
      replyCount = newEchoCount;
    } else {
      if (newEchoCount > sendCount) {
        error = 
          "Source "+source+
          " received updated reply count "+newEchoCount+
          " from "+target+
          " that is > the source-side send count "+sendCount;
        return Relay.RESPONSE_CHANGE;
      }
      if (newEchoCount == (1 + replyCount)) {
        // typical case, incremented by one
        ++replyCount;
      } else if (newEchoCount == replyCount) {
        // target restart?  should respond to be safe.
      } else {
        error = 
          "Source "+source+
          " received updated reply count "+newEchoCount+
          " from "+target+
          " that is != to the source-side reply count "+
          replyCount+" or 1 + the source-side reply count";
        return Relay.RESPONSE_CHANGE;
      }
    }
    replyTime = System.currentTimeMillis();
    return Relay.RESPONSE_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    if (error != null) {
      return error;
    }
    return new PingData(
        ignoreRollback, -1, false,
        echoCount, echoFillerSize, echoFillerRand);
  }
  public int updateContent(Object content, Token token) {
    // assert content != null
    PingData pingData = (PingData) content;
    int newSendCount = pingData.getCount();
    // check new send count
    if (ignoreRollback) {
      echoCount = newSendCount;
    } else {
      if (newSendCount == (echoCount + 1)) {
        // typical case, incremented by one
        echoCount++;
      } else if (newSendCount == echoCount) {
        // source restart?  should reply to be safe.
      } else {
        // either skipped one or reverted
        error = 
          "Target "+target+
          " received updated send count "+newSendCount+
          " from "+source+
          " that is != to either the target-side echo count "+
          echoCount+" or 1 + the target-side echo count";
        return (Relay.CONTENT_CHANGE | Relay.RESPONSE_CHANGE);
      }
    }
    return (Relay.CONTENT_CHANGE | Relay.RESPONSE_CHANGE);
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof PingImpl)) { 
      return false;
    } else {
      UID u = ((PingImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  private void readObject(java.io.ObjectInputStream os) 
    throws ClassNotFoundException, java.io.IOException {
      os.defaultReadObject();
      cacheTargets();
    }
  public String toString() {
    return 
      "Ping {"+
      "\n uid:            "+uid+
      "\n source:         "+source+
      "\n target:         "+target+
      "\n delayM:         "+delayMillis+
      "\n timeoutM:       "+timeoutMillis+
      "\n eventM:         "+eventMillis+
      "\n eventC:         "+eventCount+
      "\n ignoreR:        "+ignoreRollback+
      "\n limit:          "+limit+
      "\n sendFiller:     "+sendFillerSize+
      "\n sendFillerRand: "+sendFillerRand+
      "\n echoFiller:     "+echoFillerSize+
      "\n echoFillerRand: "+echoFillerRand+
      "\n sendT:          "+sendTime+
      "\n replyT:         "+replyTime+
      "\n send:           "+sendCount+
      "\n echo:           "+echoCount+
      "\n reply:          "+replyCount+
      "\n error:          "+error+
      "\n statCount:      "+statCount+
      "\n statMinRTT:     "+statMinRTT+
      "\n statMaxRTT:     "+statMaxRTT+
      "\n statSumRTT:     "+statSumRTT+
      "\n statSumSqrRTT:  "+statSumSqrRTT+
      "\n statMeanRTT:    "+getStatMeanRTT()+
      "\n statStdDevRTT:  "+getStatStdDevRTT()+
      "\n}";
  }

  private static class PingData implements Serializable {
    // from PingImpl constructor:
    private final boolean ignoreRollback;
    private final boolean echoFillerRand;
    private final int echoFillerSize;
    // dynamic counter:
    private final int count;
    // filler bytes
    private final byte[] filler;
    public PingData(
        boolean ignoreRollback,
        int echoFillerSize, boolean echoFillerRand,
        int count, int fillerSize, boolean fillerRand) {
      this.ignoreRollback = ignoreRollback;
      this.echoFillerSize = echoFillerSize;
      this.echoFillerRand = echoFillerRand;
      this.count = count;
      this.filler = alloc(fillerSize, fillerRand);
    }
    public boolean isIgnoreRollback() {
      return ignoreRollback;
    }
    public int getEchoFillerSize() {
      return echoFillerSize;
    }
    public boolean isEchoFillerRandomized() {
      return echoFillerRand;
    }
    public int getCount() { 
      return count;
    }
    public int getFillerSize() { 
      return (filler != null ? filler.length : -1);
    }
    private static byte[] alloc(int fillerSize, boolean fillerRand) {
      if (fillerSize <= 0) return null;
      byte[] ret = new byte[fillerSize];
      if (fillerRand) {
        rand.nextBytes(ret);
      }
      return ret;
    }
    public String toString() {
      return 
        "PingData("+getCount()+", "+getFillerSize()+")";
    }
  }

  /**
   * Simple factory implementation.
   */
  private static class PingImplFactory 
    implements Relay.TargetFactory, Serializable {

      private final MessageAddress target;

      public PingImplFactory(MessageAddress target) {
        this.target = target;
      }

      public Relay.Target create(
          UID uid, MessageAddress source, Object content,
          Relay.Token token) {
        PingData pingData = (PingData) content;
        int sendCount = pingData.getCount();
        if ((sendCount != 0) &&
            pingData.isIgnoreRollback()) {
          // detected restart of sender
          throw new IllegalArgumentException(
              "Unable to create ping object on target "+
              target+" from source "+source+
              " with send count "+sendCount+
              " != 0");
        }
        return new PingImpl(
            uid, source, target, -1, -1,
            -1, -1,
            pingData.isIgnoreRollback(),
            -1, -1, false,
            pingData.getEchoFillerSize(),
            pingData.isEchoFillerRandomized());
      }
    }
}
