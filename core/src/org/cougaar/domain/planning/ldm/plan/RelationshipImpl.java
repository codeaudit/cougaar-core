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

import java.util.Date;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.util.TimeSpan;

/**
 * A RelationshipImpl is the encapsulation of a time phased relationship
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: RelationshipImpl.java,v 1.4 2001-08-22 20:14:15 mthome Exp $
 **/

public class RelationshipImpl extends ScheduleElementImpl 
  implements Relationship { 

  private Role myRoleA; 
  private HasRelationships myA;
  private Role myRoleB;
  private HasRelationships myB;

  /** no-arg constructor */
  public RelationshipImpl() {
    super();
  }

   /** constructor for factory use that takes the start, end, role, 
    *  direct and indirect objects 
    **/
  public RelationshipImpl(TimeSpan timeSpan, 
                          Role role1, HasRelationships object1, 
                          HasRelationships object2) {
    this(timeSpan.getStartTime(), timeSpan.getEndTime(), role1, object1, 
         object2);
  }

   /** constructor for factory use that takes the start, end, role, 
    *  direct and indirect objects 
    **/
  public RelationshipImpl(long startTime, long endTime , 
                          Role role1, HasRelationships object1, 
                          HasRelationships object2) {
    super(startTime, endTime);
    
    Role role2 = role1.getConverse();

    // Normalize on roles so that we don't end up with relationships which
    // differ only in the A/B ordering, i.e. 
    // rel1.A == rel2.B && rel1.roleA == rel2.roleB &&
    // rel2.A == rel1.B && rel2.roleA == rel1.roleB
    if (role1.getName().compareTo(role2.getName()) < 0) {
      myRoleA = role1;
      myA = object1;
      myRoleB = role2;
      myB = object2;
    } else {
      myRoleA = role2;
      myA = object2;
      myRoleB = role1;
      myB = object1;
    }
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

    if (!(object instanceof Relationship)) {
      return false;
    }

    Relationship other = (Relationship)object;

    
    return (getRoleA().equals(other.getRoleA()) &&
            getA().equals(other.getA()) &&
            getRoleB().equals(other.getRoleB()) &&
            getB().equals(other.getB()) && 
            getStartTime() == other.getStartTime() &&
            getEndTime() == other.getEndTime());
  }

  /** Role performed by HasRelationship A
   * @return Role which HasRelationships A performs
   */
  public Role getRoleA() {
    return myRoleA;
  }

  /** Role performed  by HasRelationships B
   * @return Role which HasRelationships B performs
   */
  public Role getRoleB() {
    return myRoleB;
  }

  /**
   * @return HasRelationships A
   */
  public HasRelationships getA() {
    return myA;
  }
  
  /**
   * @return HasRelationships B
   */
  public HasRelationships getB() {
    return myB;
  }

  public String toString() {
    String AStr;
    if (getA() instanceof Asset) {
      AStr = 
        ((Asset) getA()).getItemIdentificationPG().getNomenclature();
    } else if (getA() instanceof  UniqueObject) {
      AStr = ((UniqueObject)getA()).getUID().toString();
    } else {
      AStr = getA().toString();
    }

    String BStr;
    if (getB() instanceof Asset) {
      BStr = 
        ((Asset) getB()).getItemIdentificationPG().getNomenclature();
    } else if (getB() instanceof UniqueObject) {
      BStr = ((UniqueObject)getB()).getUID().toString();
    } else {
      BStr = getB().toString();
    }

    return "<start:" + new Date(getStartTime()) + 
      " end:" + new Date(getEndTime()) + 
      " roleA:" + getRoleA()+
      " A:" + AStr +
      " roleB:" + getRoleB()+
      " B:" + BStr + ">";
  }
}









