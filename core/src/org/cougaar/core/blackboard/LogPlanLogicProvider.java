/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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


package org.cougaar.core.blackboard;

import org.cougaar.core.agent.*;

import org.cougaar.core.agent.ClusterMessage;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.core.domain.Factory;
import org.cougaar.core.domain.RootFactory;

public abstract class LogPlanLogicProvider implements LogicProvider {
  protected LogPlanServesLogicProvider logplan;
  protected ClusterServesLogicProvider cluster;
  protected RootFactory ldmf;

  public LogPlanLogicProvider(LogPlanServesLogicProvider logplan,
                              ClusterServesLogicProvider cluster)
  {
    this.logplan = logplan;
    this.cluster = cluster;
    this.ldmf = cluster.getFactory();
  }

  public void init() {
  }
}