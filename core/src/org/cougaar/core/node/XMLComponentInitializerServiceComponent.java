/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.node;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * A component which creates and advertises a XMLComponentInitializerService
 * <p>
 * @see XMLComponentInitializerServiceProvider
 *
 */
public class XMLComponentInitializerServiceComponent
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
	log.info("Not loading the XMLComponentInitializer service");
      }
    } else {
      try {
	theSP = new XMLComponentInitializerServiceProvider();
      } catch (Exception e) {
	log.error("Unable to load FileComponentInitializerService", e);
      }
      if (log.isDebugEnabled())
	log.debug("Providing XML Init service");
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
