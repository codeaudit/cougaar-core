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

import org.cougaar.core.blackboard.Blackboard;
import org.cougaar.core.service.DomainForBlackboardService;

public class DomainForBlackboardServiceImpl extends DomainServiceImpl
 implements DomainForBlackboardService {

  //When cluster creates this service it will
  //pass a reference to it's PrototypeRegistryService in the form
  // of itself acting as LDMServesPlugin...
  //In the future these service may dynamically find each otehr
  public DomainForBlackboardServiceImpl(DomainManager domainManager) {
    super(domainManager);
  }
  
  public void setBlackboard(Blackboard blackboard) {
    domainManager.blackboard(blackboard);
  }

  public void invokeDelayedLPActions() {
    domainManager.invokeDelayedLPActions();
  }
}  




