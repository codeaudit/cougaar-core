/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.core.cluster;

import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;

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
