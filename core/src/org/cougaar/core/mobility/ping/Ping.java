/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
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

package org.cougaar.core.mobility.ping;

import java.io.Serializable;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A simple ping object between two agents.
 */
public interface Ping extends UniqueObject {

  //
  // These are fixed in the constructor:
  //

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Address of the agent that initiated the chat.
   */
  MessageAddress getSource();

  /**
   * Address of the agent that was contacted.
   */
  MessageAddress getTarget();

  /**
   * Get the maximum time in milliseconds after the 
   * sent-time for the reply to be set, otherwise
   * a timeout should occur.
   */
  long getTimeoutMillis();

  /**
   * True if the Ping should ignore decrement of
   * send and reply counters, which indicates a
   * possible restart of either agent or a MTS
   * error.
   */
  boolean isIgnoreRollback();

  /**
   * Sent count limit.
   */
  int getLimit();

  /**
   * Get the number of extra bytes that will be sent
   * with each source-side send-counter update.
   */
  int getSendFillerSize();

  /**
   * Get the number of extra bytes that will be sent
   * with each target-side echo-counter update.
   */
  int getEchoFillerSize();

  //
  // The rest is dynamic:
  //

  /**
   * Time in milliseconds when the ping was created.
   * @see #recycle
   */
  long getSendTime();

  /**
   * Time in milliseconds when the ping reply was
   * received.
   * @see #recycle
   */
  long getReplyTime();

  /**
   * Source-size send counter, which starts at zero.
   */
  int getSendCount();

  /**
   * Target-side counter of received send-counters.
   */
  int getEchoCount();
  
  /**
   * Source-side counter of received echo counters.
   */
  int getReplyCount();

  /**
   * Error message if the counters are off, either due
   * to lost or duplicate pings.
   * <p>
   * If <tt>(isIgnoreRollback() == true)</tt> then this 
   * will always be null.
   */
  String getError();

  /**
   * Sender-side modifier, which resets the send-time,
   * reply-time, and increments the send counter.
   */
  void recycle();

  /**
   * Sender-side modifier, to set an error message.
   */
  void setError(String error);

}
