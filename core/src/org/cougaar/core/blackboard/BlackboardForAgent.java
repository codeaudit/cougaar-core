/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.blackboard;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.cluster.persist.Persistence;

/**
 * The service which the Blackboard serves back to the Agent.
 **/
public interface BlackboardForAgent
  extends Service
{
  // might be better for blackboard to be a messagetransport client, eh?
  void receiveMessages(List messages);
  // not sure if this is really needed - check out ClusterImpl.getDatabaseConnection()
  Persistence getPersistence();
}
