/*
 * <copyright>
 *  Copyright 2002 BBNT Solutions, LLC
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

import org.cougaar.core.plugin.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.util.*;
import java.util.*;
import org.cougaar.core.service.*;
import org.cougaar.core.node.*;
import org.cougaar.core.agent.*;

/** A very simple Component which can be plugged into just about any
 * non-descriminating insertion points in the system.  When loaded, it
 * prints a short message describing the node and agent where it is loaded
 * and then dumps a stack. <br>
 * This class is very useful for debugging the component model and load process.
 * It is here (in the core module) because it depends on core cougaar (identification) services,
 **/
public class DummyComponent
  extends ComponentSupport
{
  String p = "unknown";
  public void setParameter(Object o) {
    if (o instanceof Collection) {
      Iterator it =((Collection)o).iterator(); 
      if (it.hasNext()) p = (String)it.next();
    }
  }

  public void load() {
    ServiceBroker sb = getBindingSite().getServiceBroker();

    // what node are we in?
    String nn = "?";
    {
      NodeIdentificationService nis = (NodeIdentificationService) sb.getService(this,NodeIdentificationService.class,null);
      if (nis != null) {
        nn = nis.getMessageAddress().toString();
      }
    }

    // what agent?
    String an = "?";
    {
      AgentIdentificationService ais = (AgentIdentificationService) sb.getService(this,AgentIdentificationService.class,null);
      if (ais != null) {
        an = ais.getName();
      }
    }

    System.err.println("Loading DummyComponent("+p+") in "+nn+"/"+an+":");
    Thread.dumpStack();
    super.load();
  }
}
