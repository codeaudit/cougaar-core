/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.core.domain;

import java.util.List;
import java.util.Set;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;

import org.cougaar.core.service.DomainForBlackboardService;

public class DomainForBlackboardServiceImpl extends DomainServiceImpl
 implements DomainForBlackboardService {

  public DomainForBlackboardServiceImpl(DomainManager domainManager) {
    super(domainManager);
  }
  
  public void setBlackboard(Blackboard blackboard) {
    domainManager.setBlackboard(blackboard);
  }

  public void invokeDelayedLPActions() {
    domainManager.invokeDelayedLPActions();
  }

  /** invoke EnvelopeLogicProviders across all currently loaded domains **/
  public void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                           boolean persistenceEnv) {
    domainManager.invokeEnvelopeLogicProviders(tuple, persistenceEnv);
  }

  /** invoke MessageLogicProviders across all currently loaded domains **/
  public void invokeMessageLogicProviders(DirectiveMessage message) {
    domainManager.invokeMessageLogicProviders(message);
  }

  /** invoke RestartLogicProviders across all currently loaded domains **/
  public void invokeRestartLogicProviders(ClusterIdentifier cid) {
    domainManager.invokeRestartLogicProviders(cid);
  }

  /** invoke ABAChangeLogicProviders across all currently loaded domains **/
  public void invokeABAChangeLogicProviders(Set communities) {
    domainManager.invokeABAChangeLogicProviders(communities);
  }

}  




