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
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingControlService;
import org.cougaar.core.logging.LoggingServiceProvider;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The LoggingServiceComponent adds the {@link LoggingService}
 * to the root-level service broker.
 */
public final class LoggingServiceComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private ServiceProvider lsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    NodeControlService ncs = (NodeControlService)
      sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    sb.releaseService(this, NodeControlService.class, ncs);

    lsp = new LoggingServiceProvider();
    rootsb.addService(LoggingService.class, lsp);
    rootsb.addService(LoggingControlService.class, lsp);
  }
  
  public void unload() {
    super.unload();

    rootsb.revokeService(LoggingControlService.class, lsp);
    rootsb.revokeService(LoggingService.class, lsp);
    lsp = null;
  }
}
