/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import org.cougaar.core.society.*;
import java.util.*;

/** The binding site for talking to the Cougaar MessageTransport
 **/

public interface MessageTransportBindingSite 
  extends BindingSite
{
  void send(Message m) throws Exception;
  //...
}
