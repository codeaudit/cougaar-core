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
 * A message from a white pages cache to a server, or between
 * servers.
 */
public final class WPQuery extends WhitePagesMessage {

  public static final int LOOKUP  = 0;
  public static final int MODIFY  = 1;
  public static final int FORWARD = 2;

  private final long sendTime;
  private final int action;
  private final Map m;

  public WPQuery(
      MessageAddress source,
      MessageAddress target,
      long sendTime,
      int action,
      Map m) {
    super(source, target);
    this.sendTime = sendTime;
    this.action = action;
    this.m = m;
    // validate
    String s =
      ((sendTime < 0) ? "invalid send time: "+sendTime :
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
   * The time on the client's clock when this was sent.
   */
  public long getSendTime() {
    return sendTime;
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
      " from="+getOriginator()+
      " to="+getTarget()+
      " sent="+Timestamp.toString(sendTime, now)+
      " "+m+")";
  }
}
