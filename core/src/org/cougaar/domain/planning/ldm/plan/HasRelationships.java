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

/** HasRelationships - marker for clases which contain a relationship
  * schedule.
  * @version      $Id: HasRelationships.java,v 1.2 2001-04-05 19:27:15 mthome Exp $
  **/

public interface HasRelationships {
  
  RelationshipSchedule getRelationshipSchedule();

  void setRelationshipSchedule(RelationshipSchedule relationships);
}
