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

/** 
 * A Prepositional Phrase is part of a Task.  It contains
 * a String representation of the preposition (from, to, with, etc)
 * and an object(of type asset) that represents the indirect object.  
 **/
	
public interface PrepositionalPhrase
{
	
  /**
   * @return One of the values defined in Preposition.
   * @see org.cougaar.domain.planning.ldm.plan.Preposition for a list of valid values.
   */
  String getPreposition();
	
  /** @return Object - the IndirectObject  which  will be of type
   * Asset, Location, Schedule, Requisition, Vector, or OPLAN
   * @see org.cougaar.domain.planning.ldm.asset.Asset
   * @see org.cougaar.domain.planning.ldm.plan.Location
   * @see org.cougaar.domain.planning.ldm.plan.Schedule
   * @see org.cougaar.domain.planning.ldm.plan.Requisition
   * @see org.cougaar.domain.planning.ldm.OPlan;
   */
  Object getIndirectObject();
	
}
