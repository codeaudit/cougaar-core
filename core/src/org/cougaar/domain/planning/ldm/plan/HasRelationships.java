/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.plan;

/** HasRelationships - marker for clases which contain a relationship
  * schedule.
  * @version      $Id: HasRelationships.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
  **/

public interface HasRelationships {
  
  RelationshipSchedule getRelationshipSchedule();

  void setRelationshipSchedule(RelationshipSchedule relationships);
}
