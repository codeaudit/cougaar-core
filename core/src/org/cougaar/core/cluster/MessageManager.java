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

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.core.cluster.AckDirectiveMessage;
import java.util.Iterator;

public interface MessageManager {
  int OK      = 0;
  int RESTART = 4;
  int IGNORE  = 8;
  int DUPLICATE = IGNORE + 1;
  int FUTURE    = IGNORE + 2;
  int PRESENT   = OK;

  /**
   * Start the message manager running. The message manager should be
   * inactive until this method is called because it does know know
   * the identity of the cluster it is in.
   **/
  void start(ClusterServesLogicProvider aCluster, boolean didRehydrate);
  /**
   * Add a set of messages to the queue of messages waiting to be
   * transmitted. When persistence is enabled, the messages are held
   * until the end of the epoch.
   **/
  void sendMessages(Iterator messages);
  /**
   * Incorporate a directive message into the message manager's state.
   * @return Normally, the message is returned, but duplicate and from
   * the future messages are ignored by returning null.
   **/
  int receiveMessage(DirectiveMessage aMessage);
  /**
   * Incorporate a directive acknowledgement into the message
   * manager's state. The acknowledged messages are removed from the
   * retransmission queues.
   **/
  int receiveAck(AckDirectiveMessage theAck);
  /**
   * Prepare to acknowledge a list of directive messages. The
   * acknowledgements are not actually sent until the end of the
   * epoch.
   **/
  void acknowledgeMessages(Iterator messagesToAck);

  /**
   * Advance epoch.  Bring the current epoch to an end in preparation
   * for a persistence delta.
   * @return true if the message manager actually requires the epoch to advance
   **/
  void advanceEpoch();

  boolean needAdvanceEpoch();
}
