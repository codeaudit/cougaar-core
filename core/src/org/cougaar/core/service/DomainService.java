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

package org.cougaar.core.service;

import java.util.List;

import org.cougaar.core.domain.*;

import org.cougaar.core.agent.ClusterIdentifier;

import org.cougaar.core.blackboard.DirectiveMessage;
import org.cougaar.core.blackboard.EnvelopeTuple;

import org.cougaar.core.component.Service;

import org.cougaar.planning.ldm.plan.ClusterObjectFactory;

public interface DomainService extends Service {
  /**
   * Answer with a reference to the Factory
   * It is intended that there be one and only one ClusterObjectFactory
   * per Cluster instance.  Hence, ClusterManagment will always provide
   * plugins with access to the ClusterObjectFactory
   **/
  ClusterObjectFactory getClusterObjectFactory();

  /** expose the LDM factory instance to consumers.
   *  @return LdmFactory The fatory object to use in constructing LDM Objects
   **/
  RootFactory getFactory();
 
  /** @deprecated use getFactory() **/
  RootFactory getLdmFactory();

  /** return a domain-specific factory **/
  Factory getFactory(String domainname);

  /** return a list of all domain-specific factories **/
  List getFactories();

  /** invoke EnvelopeLogicProviders across all currently loaded domains **/
  void invokeEnvelopeLogicProviders(EnvelopeTuple tuple, 
                                    boolean persistenceEnv);
  /** invoke MessageLogicProviders across all currently loaded domains **/
  void invokeMessageLogicProviders(DirectiveMessage message);

  /** invoke RestartLogicProviders across all currently loaded domains **/
  void invokeRestartLogicProviders(ClusterIdentifier cid);
}  





