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
 
 /** NewAssignedRelationshipElement extends AssignedRelationshipElement and
   * provides setter methods for building valid AssignedRelationshipElement 
   * objects.
   *
   * @author  ALPINE <alpine-software@bbn.com>
   * @version $Id: NewAssignedRelationshipElement.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
   **/
 	 
public interface NewAssignedRelationshipElement extends AssignedRelationshipElement, 
  NewScheduleElement {
 	
  /** Set the string identifier for the Asset mapping to HasRelationships A in
   * the associated relationship
   * @param itemID String
   **/ 
   void setItemIDA(String itemID);

  /** Set the string identifier for the Asset mapping to HasRelationships A in
   * the associated relationship
   * @param asset Asset 
   **/ 
   void setItemIDA(Asset asset);

  /** Set the string identifier for the Asset mapping to HasRelationships B in
   * the associated relationship
   * @param itemID String
   **/ 
   void setItemIDB(String itemID);

  /** Set the string identifier for the Asset mapping to HasRelationships B in
   * the associated relationship
   * @param asset Asset
   **/ 
   void setItemIDB(Asset asset);

  /** Set the Role for the Asset identified by itemIDA
   * @param role Role
   **/
   void setRoleA(Role role);

  /** Set the Role for the Asset identified by itemIDB
   * @param role Role
   **/
   void setRoleB(Role role);
}


