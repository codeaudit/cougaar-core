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

import org.cougaar.domain.planning.ldm.asset.Asset;

/**
 * Relationship - maps relationship between any two objects. Each object
 * has a role. The roles are presumed to be complementary.
 **/

public interface Relationship extends ScheduleElement {

  /** Role performed by HasRelationship A
   * @return Role which HasRelationships A performs
   */
  public Role getRoleA();

  /**
   * 
   * @return HasRelationships A
   */
  public HasRelationships getA();

  /** Role performed  by HasRelationships B
   * @return Role which HasRelationships B performs
   */
  public Role getRoleB();
  
  /**
   * @return HasRelationships B
   */
  public HasRelationships getB();

}






