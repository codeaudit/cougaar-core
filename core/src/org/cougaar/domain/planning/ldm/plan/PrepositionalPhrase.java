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
