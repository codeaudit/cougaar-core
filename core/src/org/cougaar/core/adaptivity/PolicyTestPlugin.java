package org.cougaar.core.adaptivity;

import java.io.InputStream;
import java.io.IOException;
import java.io.StreamTokenizer;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.util.GenericStateModelAdapter;

public class PolicyTestPlugin extends ServiceUserPluginBase {
  private PlaybookConstrainService PlaybookConstrainService;

  private LoggingService logger;

  private boolean constrained = false;

  private OperatingModePolicy[] policies;

  private static final Class[] requiredServices = {
    LoggingService.class,
    PlaybookConstrainService.class
  };

  public PolicyTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    String policyFileName = getParameters().iterator().next().toString();
    try {
      InputStream is = getConfigFinder().open(policyFileName);
      try {
        Parser p = new Parser(new StreamTokenizer(is));
        policies = p.parseOperatingModePolicies();
      } finally {
        is.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing policy file", e);
    }
    if (haveServices()) setPolicies();
  }

  private boolean haveServices() {
    if (logger != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      logger = (LoggingService)
        sb.getService(this, LoggingService.class, null);
      PlaybookConstrainService = (PlaybookConstrainService)
        sb.getService(this, PlaybookConstrainService.class, null);
      return true;
    }
    return false;
  }

  public void execute() {
    if (timerExpired()) {
      if (haveServices()) {
        cancelTimer();
        setPolicies();
      }
    }
  }

  private void setPolicies() {
    if (constrained) {
      if (logger.isInfoEnabled()) logger.info("Adding threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        PlaybookConstrainService.constrain(policies[i]);
      }
      constrained = false;
    } else {
      if (logger.isInfoEnabled()) logger.info("Removing threatcon policy");
      for (int i = 0; i < policies.length; i++) {
        PlaybookConstrainService.unconstrain(policies[i]);
      }
      constrained = true;
    }
    startTimer(75000);
  }
}
