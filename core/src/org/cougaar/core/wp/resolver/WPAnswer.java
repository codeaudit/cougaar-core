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

package org.cougaar.core.wp.resolver;

import java.util.Map;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.WhitePagesMessage;

/**
 * A message from a white pages server to a client, or between
 * servers.
 */
public final class WPAnswer extends WhitePagesMessage {

  public static final int LOOKUP  = 0;
  public static final int MODIFY  = 1;
  public static final int FORWARD = 2;

  private final long sendTime;
  private final long replyTime;
  private final boolean useServerTime;
  private final int action;
  private final Map m;

  public WPAnswer(
      MessageAddress source,
      MessageAddress target,
      long sendTime,
      long replyTime,
      boolean useServerTime,
      int action,
      Map m) {
    super(source, target);
    this.sendTime = sendTime;
    this.replyTime = replyTime;
    this.useServerTime = useServerTime;
    this.action = action;
    this.m = m;
    // validate
    String s =
      ((sendTime <= 0) ? "invalid send time: "+sendTime :
       (replyTime <= 0) ? "invalid reply time: "+replyTime :
       (m == null) ? "null map" :
       (action != LOOKUP &&
        action != MODIFY &&
        action != FORWARD) ? "invalid action: "+action : 
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * The time on the client's clock when the query was sent.
   */
  public long getSendTime() {
    return sendTime;
  }

  /**
   * The time on the server's clock when this response was sent.
   */
  public long getReplyTime() {
    return replyTime;
  }

  /**
   * If true, then all timestamp offsets are relative to the server's
   * reply time, otherwise they should be based upon the client's
   * send time plus half the client's measured round-trip-time.
   * <p>
   * This flag is chosen by the server.  The advantage of this flag:
   * <ol>
   *   <li><i>true</i> (server time):<br>
   *       This assumes that the client's clock is well
   *       synchronized with the server's clock (e.g. NTP).</li>
   *   <li><i>false</i> (client time):<br>
   *       This avoids clock synchronization issues but assumes
   *       quick message delivery and approximately equal message
   *       send/reply delivery times.</li>
   * </ol>
   */
  public boolean useServerTime() {
    return useServerTime;
  }

  /**
   * @return the action of request
   */
  public int getAction() {
    return action;
  }

  /**
   * The content of this message.
   */
  public Map getMap() {
    return m;
  }

  public String toString() {
    long now = System.currentTimeMillis();
    return toString(now);
  }

  public String toString(long now) {
    return 
      "("+
      (action == LOOKUP ? "lookup" :
       action == MODIFY ? "modify" :
       "forward")+
      "-answer from="+getOriginator()+
      " to="+getTarget()+
      " sent="+Timestamp.toString(sendTime, now)+
      " reply="+Timestamp.toString(replyTime, now)+
      " useServer="+useServerTime+
      " "+m+")";
  }
}
