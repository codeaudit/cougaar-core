/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.adaptivity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.ConfigFinder;

/**
 * Test plugin that reads policies from a file, creates 
 * InterAgentOperatingModePolicies and alternately forwards 
 * the policy to another agent and revokes the policy.
 *
 **/
public class PolicyRemoteTestPlugin extends ServiceUserPluginBase {

  private boolean published = false;

  private InterAgentOperatingModePolicy[] policies;

  private UIDService uidService;

  private static final Class[] requiredServices = {
    UIDService.class
  };

  public PolicyRemoteTestPlugin() {
    super(requiredServices);
  }

  public void setupSubscriptions() {
    OperatingModePolicy[] tempPolicies = null;
    String policyFileName = getParameters().iterator().next().toString();
    try {
      Reader r = new InputStreamReader(getConfigFinder().open(policyFileName));
      try {
        Parser p = new Parser(r, logger);
        tempPolicies = p.parseOperatingModePolicies();
      } finally {
        r.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing policy file", e);
    }

    policies = new InterAgentOperatingModePolicy[tempPolicies.length];
    for (int i = 0; i < tempPolicies.length; i++) {
      InterAgentOperatingModePolicy iaomp 
	= new InterAgentOperatingModePolicy(tempPolicies[i].getName(),
					    tempPolicies[i].getIfClause(), 
					    tempPolicies[i].getOperatingModeConstraints(),
					    getAgentIdentifier().toString());

      policies[i] = iaomp;
      if (haveServices()) {
  	uidService.registerUniqueObject(policies[i]);
      }
    }
    setPolicies();

  }

  public void execute() {
    if (timerExpired()) {
      cancelTimer();
      setPolicies();
    }
  }

  private boolean haveServices() {
    if (uidService != null) return true;
    if (acquireServices()) {
      ServiceBroker sb = getServiceBroker();
      uidService = (UIDService)
        sb.getService(this, UIDService.class, null);
      return true;
    }
    return false;
  }

  private void setPolicies() {
    if (!published) {
      if (logger.isInfoEnabled()) logger.info("publishing policy");
      for (int i = 0; i < policies.length; i++) {
 	policies[i].setTarget(MessageAddress.getMessageAddress("Provider"));
	getBlackboardService().publishAdd(policies[i]);
	published = true;
      }
    } else {
      if (logger.isInfoEnabled()) logger.info("Removing policy");
      for (int i = 0; i < policies.length; i++) {
  	//policies[i].setTarget((MessageAddress)null);
	//getBlackboardService().publishChange(policies[i]);        
  	getBlackboardService().publishRemove(policies[i]);        
      }
      published = false;
    }
    startTimer(75000);
  }
}
