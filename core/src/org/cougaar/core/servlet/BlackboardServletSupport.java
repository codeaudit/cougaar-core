package org.cougaar.core.servlet;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.domain.*;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.servlet.SimpleServletSupportImpl;
import org.cougaar.util.ConfigFinder;

/** 
 * <pre>
 * This support class offers additional services on top of the
 * SimpleServletSupport class, including access to the blackboard,
 * config finder, root factory, ldm serves plugin, and scheduler service.
 * </pre>
 */
public class BlackboardServletSupport extends SimpleServletSupportImpl {
  public BlackboardServletSupport(
      String path,
      ClusterIdentifier agentId,
      BlackboardService blackboard,
      NamingService ns,
      LoggingService logger,
      ConfigFinder configFinder,
      RootFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler) {
    super (path, agentId, blackboard, ns, logger);
    this.configFinder = configFinder;
    this.ldmf = ldmf;
    this.ldm = ldm;
    this.scheduler = scheduler;
  }
  protected ConfigFinder configFinder;
  protected RootFactory ldmf;
  protected LDMServesPlugin ldm;
  protected SchedulerService scheduler;

  // I need access to the blackboard so I can publish to it
  public BlackboardService getBlackboardService () { return blackboard; }
  public ConfigFinder getConfigFinder () { return configFinder; }
  public RootFactory getLDMF () { return ldmf; }
  public LDMServesPlugin getLDM () { return ldm; }
  public SchedulerService getSchedulerService () { return scheduler; }
}
