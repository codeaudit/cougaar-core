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

/**
 * A BinderFactory for binding Agents to the AgentManager.
 **/
public class AgentBinderFactory 
  extends BinderFactorySupport
{

  /** AgentBinderFactory always uses AgentBinder.
   **/
  public Class getBinderClass(Object child) {
    //Might want to differentiate between Agent and specializations of
    //agents such as Clusters at some point.  But for now...
    return AgentBinder.class;
  }
}

