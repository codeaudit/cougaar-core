/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Iterator;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.plugin.PluginBindingSite;
import org.cougaar.core.persist.NotPersistable;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.LoggingService;

/**
 * Plugin that reads OperatingModePolicies from files and publishes them
 * to the blackboard
 **/
public class PolicyInjectorPlugin extends ComponentPlugin {

  private LoggingService logger;

  private OperatingModePolicy[] policies;

  private UIDService uidService = null;

  public void setUIDService(UIDService service) {
    uidService = service;
  }

  public void setLoggingService (LoggingService ls) {
    logger = ls;
  }

  public void setupSubscriptions() {
    String here = ((PluginBindingSite)getBindingSite()).getAgentIdentifier().toString();
    for (Iterator fileIterator = getParameters().iterator(); 
	 fileIterator.hasNext();) {
      String policyFileName = fileIterator.next().toString();
      try {
	Reader is = new InputStreamReader(getConfigFinder().open(policyFileName));
	try {
	  Parser p = new Parser(is, logger);
	  policies = p.parseOperatingModePolicies();
	} finally {
	  is.close();
	}
      } catch (Exception e) {
	logger.error("Error parsing policy file " + policyFileName, e);
      }
    }
    for (int i=0; i<policies.length; i++) {
      policies[i].setAuthority(here);
      uidService.registerUniqueObject(policies[i]);
      blackboard.publishAdd(policies[i]);
    }
  }
  
  public void execute() {
  }

}
