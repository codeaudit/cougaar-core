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


package org.cougaar.domain.planning.ldm.plan;

/** Special Interface to PlanElement for Assessors only.
 * In particular, only plugins which provide alp-external access
 * to a given asset should call these methods.  For example, the 
 * infrastructure relies on this interface to propagate allocation
 * information between clusters for organizations.
 *
 * Note that while all PlanElements implement this interface,
 * PlanElement does not extend this interface, thus forcing 
 * Assessors to cast to this class. 
 *
 * In no case should a plugin cast PlanElements to any type
 * in the alp package tree.
 **/

public interface PlanElementForAssessor extends PlanElement {
  
  /** @param rcvres set the received AllocationResult object associated 
   * with this plan element.
   **/
  void setReceivedResult(AllocationResult rcvres);
  
  /**
   * @param repres set the reported AllocationResult object associated 
   * with this plan element.
   * @deprecated used setReceivedResult instead 
   **/
  void setReportedResult(AllocationResult repres);
  
}
