package org.cougaar.core.node;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * A component which creates and advertises a FileComponentInitializerService
 * <p>
 * @see FileComponentInitializerServiceProvider
 *
 */
public class FileComponentInitializerServiceComponent
 extends GenericStateModelAdapter
 implements Component {
  private ServiceBroker sb;

  private ServiceProvider theSP;
  private LoggingService log;

  public void setBindingSite(BindingSite bs) {
    // this is the *node* service broker!  The NodeControlService
    // is not available until the node-agent is created...
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();
    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);
    if (log == null) {
      log = LoggingService.NULL;
    }

    if (sb.hasService(ComponentInitializerService.class)) {
      // already have a ComponentInitializer? 
      // Leave the existing one in place
      if (log.isInfoEnabled()) {
	log.info("Not loading the FileComponentInitializer service");
      }
    } else {
      try {
	theSP = new FileComponentInitializerServiceProvider();
      } catch (Exception e) {
	log.error("Unable to load FileComponentInitializerService", e);
      }
      if (log.isDebugEnabled())
	log.debug("Providing File (INI) Init service");
      sb.addService(ComponentInitializerService.class, theSP);
    }

    if (log != LoggingService.NULL) {
      sb.releaseService(this, LoggingService.class, log);
      log = null;
    }
  }

  public void unload() {
    if (theSP != null) {
      sb.revokeService(ComponentInitializerService.class, theSP);
      theSP = null;
    }
    super.unload();
  }
}
