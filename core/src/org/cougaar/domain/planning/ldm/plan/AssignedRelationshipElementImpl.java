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

/**
 * AssignedRelationshipElementImpl represents a relationship between assets.
 * Used instead of Relationships in the AssetAssignment directive to prevent
 * deadlock problems. Assets in the relationships represented by the
 * ItemIdentification from their ItemIdentificationPG
 *
 * @author  ALPINE <alpine-software@bbn.com>
 *
 **/

public class AssignedRelationshipElementImpl extends ScheduleElementImpl
  implements NewAssignedRelationshipElement {

  private String myItemIDA;
  private String myItemIDB;
  private Role myRoleA;
  private Role myRoleB;

  /** constructor for factory use */
  public AssignedRelationshipElementImpl() {
    super();
    setItemIDA("");
    setItemIDB("");
    setRoleA(Role.BOGUS);
    setRoleB(Role.BOGUS);
  }

  /** constructor for factory use that takes assets, roles, start, end times
  **/
  public AssignedRelationshipElementImpl(Asset assetA, Role roleA,
                                         Asset assetB, Role roleB,
                                         long start, long end) {
    super(start, end);
    setItemIDA(assetA);
    setRoleA(roleA);
    setItemIDB(assetB);
    setRoleB(roleB);
  }

  /** constructor for factory use that takes a Relationship
  **/
  public AssignedRelationshipElementImpl(Relationship relationship) {
    this((Asset)relationship.getA(),
         relationship.getRoleA(),
         (Asset)relationship.getB(),
         relationship.getRoleB(),
         relationship.getStartTime(),
         relationship.getEndTime());
  }
       
  /** String identifier for the Asset mapping to HasRelationships A in the 
   * associated relationship
   * @return String
   **/ 
  public String getItemIDA() { 
    return myItemIDA; 
  }

  /** Set the string identifier for the Asset mapping to  HasRelationships A 
   * in the associated relationship
   * @param itemID String
   **/ 
  public void setItemIDA(String itemID) {
    myItemIDA = itemID;
  }

  /** Set the string identifier for the Asset mapping to HasRelationships A 
   * in the associated relationship. 
   * @param asset Asset  
   **/ 
  public void setItemIDA(Asset asset) {
    myItemIDA = asset.getItemIdentificationPG().getItemIdentification();
  }

  /** String identifier for the Asset mapping to HasRelationships B in the
   * associated relationship
   * @return String
   **/
  public String getItemIDB() { 
    return myItemIDB; 
  }

  /** Set the string identifier for the Asset mapping to HasRelationships B 
   * in the associated relationship
   * @param String
   **/ 
  public void setItemIDB(String itemID) {
    myItemIDB = itemID;
  }

  /** Set the string identifier for the Asset mapping to HasRelationships B 
   * in the associated relationship
   * @param asset Asset
   **/ 
  public void setItemIDB(Asset asset) {
    myItemIDB = asset.getItemIdentificationPG().getItemIdentification();
  }

  /** Role for the Asset identified by itemIDA
   * @return Role
   **/
  public Role getRoleA() { 
    return myRoleA; 
  }

  /** Set the Role for the Asset identified by itemIDA
   * @param role Role
   **/
  public void setRoleA(Role role) {
    myRoleA = role;
  }

  /** Role for the Asset identified by itemIDB
   * @return Role
   **/
  public Role getRoleB() { 
    return myRoleB; 
  }

  /** Set the Role for the Asset identified by itemIDB
   * @param role Role
   **/
  public void setRoleB(Role role) {
    myRoleB = role;
  }

  /** 
   * equals - performs field by field comparison
   *
   * @param object Object to compare
   * @return boolean if 'same' 
   */
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if (!(object instanceof AssignedRelationshipElement)) {
      return false;
    }

    AssignedRelationshipElement other = (AssignedRelationshipElement)object;

    
    return (getItemIDA().equals(other.getItemIDA()) &&
            getRoleA().equals(other.getRoleA()) &&
            getItemIDB().equals(other.getItemIDB()) && 
            getRoleB().equals(other.getRoleB()) &&
            getStartTime() == other.getStartTime() &&
            getEndTime() == other.getEndTime());
  }

  public String toString() {
    return super.toString() + "::"
      + getItemIDA() + "/" + getRoleA() + "::"
      + getItemIDB() + "/" + getRoleB();
  }
}
