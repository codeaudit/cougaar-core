
package org.cougaar.planning.servlet;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.planning.ldm.LDMServesPlugin;
import org.cougaar.planning.ldm.PlanningFactory;
import org.cougaar.core.service.BlackboardQueryService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.NamingService;
import org.cougaar.core.service.SchedulerService;
import org.cougaar.core.servlet.*;
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
      MessageAddress agentId,
      BlackboardQueryService blackboardQuery,
      NamingService ns,
      LoggingService logger,
      BlackboardService blackboard,
      ConfigFinder configFinder,
      PlanningFactory ldmf,
      LDMServesPlugin ldm,
      SchedulerService scheduler) {
    super (path, agentId, blackboardQuery, ns, logger);
    this.blackboard = blackboard;
    this.configFinder = configFinder;
    this.ldmf = ldmf;
    this.ldm = ldm;
    this.scheduler = scheduler;
  }
  protected BlackboardService blackboard;
  protected ConfigFinder configFinder;
  protected PlanningFactory ldmf;
  protected LDMServesPlugin ldm;
  protected SchedulerService scheduler;

  // I need access to the blackboard so I can publish to it
  public BlackboardService getBlackboardService () { return blackboard; }
  public ConfigFinder getConfigFinder () { return configFinder; }
  public PlanningFactory getLDMF () { return ldmf; }
  public LDMServesPlugin getLDM () { return ldm; }
  public SchedulerService getSchedulerService () { return scheduler; }
}
