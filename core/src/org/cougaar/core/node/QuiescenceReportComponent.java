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

import org.cougaar.core.agent.AgentContainer;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.QuiescenceReportForDistributorService;
import org.cougaar.core.service.QuiescenceReportService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The QuiescenceReportComponent adds the
 * {@link QuiescenceReportService} to the root-level service broker.
 */ 
public final class QuiescenceReportComponent
extends GenericStateModelAdapter
implements Component
{

  private ServiceBroker sb;
  private ServiceBroker rootsb;

  private LoggingService log;
  private QuiescenceReportService quiescenceReportService;

  private MessageAddress localAgent = null;

  private QuiescenceReportServiceProvider qrsp;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void load() {
    super.load();

    log = (LoggingService)
      sb.getService(this, LoggingService.class, null);

    AgentContainer agentContainer;

    NodeControlService ncs = (NodeControlService)
      sb.getService(this, NodeControlService.class, null);
    if (ncs == null) {
      throw new RuntimeException("Unable to obtain NodeControlService");
    }
    rootsb = ncs.getRootServiceBroker();
    agentContainer = ncs.getRootContainer();
    sb.releaseService(this, NodeControlService.class, ncs);

    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    if (ais != null) {
      localAgent = ais.getMessageAddress();
      sb.releaseService(
          this, AgentIdentificationService.class, ais);
    }

    qrsp = 
      new QuiescenceReportServiceProvider(
          localAgent.getAddress(),
          agentContainer,
          sb);
    rootsb.addService(QuiescenceReportService.class, qrsp);
    rootsb.addService(QuiescenceReportForDistributorService.class, qrsp);

    // mark our node as non-quiescent until we are started, which
    // will occur after all other components have been loaded.
    //
    // note that we pass our MessageAddress as the requestor.
    quiescenceReportService = (QuiescenceReportService)
      sb.getService(
          localAgent, QuiescenceReportService.class, null);

    quiescenceReportService.clearQuiescentState();
  }

  public void start() {
    super.start();

    if (quiescenceReportService != null) {
      quiescenceReportService.setQuiescentState();
      sb.releaseService(
          localAgent,
          QuiescenceReportService.class,
          quiescenceReportService);
      quiescenceReportService = null;
    }
  }

  public void unload() {
    super.unload();

    rootsb.revokeService(QuiescenceReportService.class, qrsp);
    qrsp = null;
  }
}
