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


/**
 * NewRelationshipSchedule - provides setters to build a complete object
 * It is a real bad idea to change schedules unless you are the 
 * "owner" of a schedule (e.g. you actually created it).
 **/

public interface NewRelationshipSchedule 
  extends RelationshipSchedule, NewSchedule {
  /**
   * Should only be called when the Schedule is empty.
   * @param hasRelationships HasRelationships whose relationships are 
   * contained in the schedule
   */
  void setHasRelationships(HasRelationships hasRelationships);
}

