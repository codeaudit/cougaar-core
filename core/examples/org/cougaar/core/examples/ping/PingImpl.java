/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.examples.ping;

import java.io.Serializable;
import java.util.Set;
import java.util.Collections;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.relay.Relay;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.util.XMLize;

/**
 * Package-private implementation of a Ping.
 * <p>
 * This uses the Relay support to transfer the data
 * between the source agent and target agent.
 *
 * @deprecated moved to "org.cougaar.core.mobility.ping.*" (bug 1792)
 */
class PingImpl 
implements Ping, Relay.Source, Relay.Target, XMLizable, Serializable {

  private final UID uid;
  private final MessageAddress source;
  private final MessageAddress target;
  private final long timeoutMillis;
  private final int limit;
  private long sendTime;
  private long replyTime;
  private int sendCount;
  private int echoCount;
  private int replyCount;
  private String error;

  private transient Set _targets;
  private transient Relay.TargetFactory _factory;

  public PingImpl(
      UID uid, 
      MessageAddress source,
      MessageAddress target,
      long timeoutMillis,
      int limit) {
    this.uid = uid;
    this.source = source;
    this.target = target;
    this.timeoutMillis = timeoutMillis;
    this.limit = limit;
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
  public long getTimeoutMillis() {
    return timeoutMillis;
  }
  public int getLimit() {
    return limit;
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
  public void setError(String error) {
    if (this.error != null) {
      throw new RuntimeException(
          "Error message already set to "+this.error);
    }
    this.error = error;
  }
  public void recycle() {
    sendTime = System.currentTimeMillis();
    replyTime = -1;
    sendCount++;
    // caller must publish-change!
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
    return new Integer(sendCount);
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
    int newEchoCount = ((Integer) response).intValue();
    // check new echo count
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
    replyTime = System.currentTimeMillis();
    return Relay.RESPONSE_CHANGE;
  }

  // Relay.Target:

  public Object getResponse() {
    if (error != null) {
      return error;
    }
    return new Integer(echoCount);
  }
  public int updateContent(Object content, Token token) {
    // assert content != null
    int newSendCount = ((Integer) content).intValue();
    // check new send count
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
    return (Relay.CONTENT_CHANGE | Relay.RESPONSE_CHANGE);
  }

  public org.w3c.dom.Element getXML(org.w3c.dom.Document doc) {
    return XMLize.getPlanObjectXML(this, doc);
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
      "\n uid:      "+uid+
      "\n source:   "+source+
      "\n target:   "+target+
      "\n timeoutM: "+timeoutMillis+
      "\n limit:    "+limit+
      "\n sendT:    "+sendTime+
      "\n replyT:   "+replyTime+
      "\n send:     "+sendCount+
      "\n echo:     "+echoCount+
      "\n reply:    "+replyCount+
      "\n error:    "+error+
      "\n}";
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
        int sendCount = ((Integer) content).intValue();
        if (sendCount != 0) {
          throw new IllegalArgumentException(
              "Unable to create ping object on target "+
              target+" from source "+source+
              " with send count "+sendCount+
              " != 0");
        }
        return new PingImpl(uid, source, target, -1, -1);
      }
    }
}
