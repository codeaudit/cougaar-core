/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.core.cluster.AckDirectiveMessage;
import java.util.Iterator;

public interface MessageManager {
  public static final int OK      = 0;
  public static final int RESTART = 4;
  public static final int IGNORE  = 8;
  public static final int DUPLICATE = IGNORE + 1;
  public static final int FUTURE    = IGNORE + 2;
  public static final int PRESENT   = OK;
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
