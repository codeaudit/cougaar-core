/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.agent;

import java.util.*;
import java.lang.reflect.*;
import org.cougaar.core.component.*;
import org.cougaar.core.society.NodeForBinder;

/**
 * A BinderFactory for binding AgentManagers to Nodes.
 **/
public class AgentManagerBinderFactory extends BinderFactorySupport
{
  /** AgentManagerBinderFactory always uses AgentManagerBinder.
   **/
  public Class getBinderClass(Object child) {
    return AgentManagerBinder.class;
  }
}

