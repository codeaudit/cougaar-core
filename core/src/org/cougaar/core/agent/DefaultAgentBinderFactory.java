/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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
package org.cougaar.core.agent;

import org.cougaar.core.blackboard.*;

import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.component.*;

/**
 * The default factory for binding Agents to the AgentManager.
 **/
public class DefaultAgentBinderFactory 
  extends BinderFactorySupport
{

  /** 
   * Binder must implement AgentBinder.
   **/
  public Class getBinderClass(Object child) {
    if (child instanceof ComponentDescription) {
      ComponentDescription cd = (ComponentDescription) child;
      if (Agent.INSERTION_POINT.equals(cd.getInsertionPoint())) {
        //Might want to differentiate between Agent and specializations of
        //agents such as Clusters at some point.  But for now...
        return DefaultAgentBinder.class;
      }
    } else {
      if (child instanceof Agent) {
        return DefaultAgentBinder.class;
      }
    }
 
    // otherwise 
    return null;
  }
}

