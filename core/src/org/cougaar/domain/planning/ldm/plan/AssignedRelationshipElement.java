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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.TimeSpan;

/**
 * AssignedRelationshipElement represents a relationship between assets. 
 * Used instead of Relationships in the AssetAssignment directive to prevent
 * deadlock problems. Assets in the relationships represented by the
 * ItemIdentification from their ItemIdentificationPG
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AssignedRelationshipElement.java,v 1.1 2000-12-15 20:16:43 mthome Exp $
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





