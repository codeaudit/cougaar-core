/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.examples;

import org.cougaar.core.component.*;
import org.cougaar.core.node.*;
import org.cougaar.core.service.AgentIdentificationService;

/** Trivial component which does nothing but print a message when it is loaded and 
 * dumps a stack to indicate from where the code is being invoked.
 * Will try to print information about which agent and node it is in (if known at load point),
 * and any parameters it was invoked with.
 **/
public class TestComponent
  extends ComponentSupport
{
  private Object param = null;

  public void setParameter(Object o) {
    param = o;
  }

  public void load() {
    super.load();
    ServiceBroker sb = getServiceBroker();
    
    String nodeName = "unknown";
    {
      NodeIdentificationService nis =
        (NodeIdentificationService) sb.getService(this, NodeIdentificationService.class, null);
      if (nis != null) {
        nodeName = nis.getMessageAddress().toString();
      }
    }

    String agentName = "unknown";
    {
      AgentIdentificationService ais =
        (AgentIdentificationService) sb.getService(this, AgentIdentificationService.class, null);
      if (ais != null) {
        agentName = ais.getName();
      }
    }

    System.err.println("Loaded ComponentTest("+param+") into "+nodeName+"/"+agentName+":");
    Thread.dumpStack();
  }

}
