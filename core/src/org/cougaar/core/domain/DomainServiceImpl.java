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
import org.cougaar.core.service.DomainService;

public class DomainServiceImpl implements DomainService {

  private Domain rootDomain = null;
  protected DomainManager domainManager = null;

  public DomainServiceImpl(DomainManager domainManager) {
    this.domainManager = domainManager;
 }

  /** return a domain-specific factory **/
  public Factory getFactory(String domainName) {
    return domainManager.getFactoryForDomain(domainName);
  }

  /** return a domain-specific factory **/
  public Factory getFactory(Class domainClass) {
    return domainManager.getFactoryForDomain(domainClass);
  }

  /** return a List of all domain-specific factories **/
  public List getFactories() {
    return domainManager.getFactories();
  }


}  




