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
import org.cougaar.core.component.*;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageTransportException;
import org.cougaar.core.society.MessageTransportService;

/** An Agent's view of its parent component (Container).
 *
 **/
public interface AgentBindingSite 
  extends BindingSite
{
  void sendMessage(Message message) throws MessageTransportException;
  MessageTransportService getMessageTransportServer();
  String getName();
}


