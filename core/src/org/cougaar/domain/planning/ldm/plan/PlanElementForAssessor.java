/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
