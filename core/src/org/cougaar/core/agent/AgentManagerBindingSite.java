/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.MessageTransportException;
import org.cougaar.core.mts.MessageTransportService;
import org.cougaar.core.society.Message;

/** An agentmanager's view of its parent component (Container).
 *
 **/
public interface AgentManagerBindingSite 
  extends BindingSite
{
  MessageTransportService getMessageTransportServer();
  void sendMessage(Message message) throws MessageTransportException;
  String getIdentifier();
  void registerCluster(ClusterServesClusterManagement cluster);
  //name of the node
  String getName();
}


