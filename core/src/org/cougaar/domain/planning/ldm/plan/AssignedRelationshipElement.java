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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.TimeSpan;

/**
 * AssignedRelationshipElement represents a relationship between assets. 
 * Used instead of Relationships in the AssetAssignment directive to prevent
 * deadlock problems. Assets in the relationships represented by the
 * ItemIdentification from their ItemIdentificationPG
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AssignedRelationshipElement.java,v 1.3 2001-08-22 20:14:10 mthome Exp $
 **/


public interface AssignedRelationshipElement extends ScheduleElement
{
	
  /** String identifier for the Asset mapping to HasRelationships A in the 
   * associated relationship
   * @return String
   **/
   String getItemIDA();

  /** String identifier for the Asset mapping to HasRelationships B in the 
   * associated relationship
   * @return String
   **/
   String getItemIDB();

  /** Role for the Asset identified by itemIDA
   * @return Role
   **/
   Role getRoleA();

  /** Role for the Asset identified by itemIDB
   * @return Role
   **/
   Role getRoleB();
} 





