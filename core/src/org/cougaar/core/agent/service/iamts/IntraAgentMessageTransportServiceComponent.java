/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.agent.service.iamts;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.service.IntraAgentMessageTransportService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The IntraAgentMessageTransportServiceComponent is a provider class for the
 * IntraAgentMessageTransport service within an agent.
 * <p>
 * This is essentially a back-pointer to the agent's MTS.
 */
public final class IntraAgentMessageTransportServiceComponent 
extends GenericStateModelAdapter
implements Component 
{
  
  // only allow the blackboard to obtain this service!
  private static final String VALID_REQUESTOR_CLASSNAME =
    "org.cougaar.core.blackboard.StandardBlackboard";

  private ServiceBroker sb;

  private LoggingService loggingS;

  private IntraAgentMessageTransportService iamtS;
  private IntraAgentMessageTransportServiceProvider iamtSP;

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setParameter(Object o) {
    iamtS = (IntraAgentMessageTransportService) o;
  }

  public void setLoggingService(LoggingService loggingS) {
    this.loggingS = loggingS;
  }

  public void load() {
    super.load();

    if (iamtS == null) {
      throw new RuntimeException(
          "Missing IntraAgentMessageTransportService parameter");
    }

    // create and advertise our service
    this.iamtSP = new IntraAgentMessageTransportServiceProvider();
    sb.addService(IntraAgentMessageTransportService.class, iamtSP);
  }

  public void unload() {
    // revoke our service
    if (iamtSP != null) {
      sb.revokeService(IntraAgentMessageTransportService.class, iamtSP);
      iamtSP = null;
    }
    super.unload();
  }

  private class IntraAgentMessageTransportServiceProvider implements ServiceProvider {
    public Object getService(
        ServiceBroker sb, Object requestor, Class serviceClass) {
      if (IntraAgentMessageTransportService.class.isAssignableFrom(serviceClass)) {
        String reqcl = (requestor==null?"null":requestor.getClass().getName());
        if (reqcl.equals(VALID_REQUESTOR_CLASSNAME)) {
          return iamtS;
        }
        if (loggingS != null && loggingS.isErrorEnabled()) {
          loggingS.error(
              "Denied request for "+serviceClass+
              " by non-blackboard requestor "+reqcl);
        }
      }
      return null;
    }

    public void releaseService(
        ServiceBroker sb, Object requestor, 
        Class serviceClass, Object service)  {
    }
  }

}
