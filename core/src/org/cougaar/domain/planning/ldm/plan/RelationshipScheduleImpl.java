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

import java.util.Collection;
import java.util.Iterator;

import org.cougaar.util.Filters;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

import org.cougaar.core.cluster.*;
import org.cougaar.core.society.*;

import org.cougaar.domain.planning.ldm.asset.Asset;

/** 
 * A RelationshipSchedule is a representation of an object (must implement
 * HasRelationships) relationships 
 **/

public class RelationshipScheduleImpl extends ScheduleImpl 
  implements NewRelationshipSchedule {

  private HasRelationships myHasRelationships;
  
  public RelationshipScheduleImpl() {
    super();
    setScheduleType(ScheduleType.RELATIONSHIP);
    setScheduleElementType(ScheduleElementType.RELATIONSHIP);
  }

  public RelationshipScheduleImpl(HasRelationships hasRelationships) {
    this();
    
    setHasRelationships(hasRelationships);
  }

  public RelationshipScheduleImpl(HasRelationships hasRelationships, 
                                  Collection relationships) {
    this(hasRelationships);
    
    addAll(relationships);
  }
                
  /** Construct a schedule which has the same elements as the specified
   * collection.  If the specified collection needs to be sorted, it will
   * be.
   **/
  public RelationshipScheduleImpl(RelationshipSchedule schedule) {
    this(schedule.getHasRelationships(), schedule);
  }

  
  public HasRelationships getHasRelationships() {
    return myHasRelationships;
  }


  public synchronized void setHasRelationships(HasRelationships hasRelationships) {
    if (!isEmpty()) {
      throw new IllegalArgumentException("RelationshipScheduleImpl.setHasRelationships() can only be called on an empty schedule"); 
    }
    
    myHasRelationships = hasRelationships;
  }
    
  public synchronized boolean isAppropriateScheduleElement(Object o) {
    if (!super.isAppropriateScheduleElement(o)) {
      return false;
    }

    Relationship relationship = (Relationship)o;

    if ((myHasRelationships == null) ||
        ((!relationship.getA().equals(myHasRelationships)) &&
         (!relationship.getB().equals(myHasRelationships)))) {
      return false;
    }

    return true;
  }

  /** getMatchingRelationships - return all Relationships which pass the
   * specified UnaryPredicate.
   * 
   * @param predicate UnaryPredicate to use in screening Relationships
   * @return a sorted Collection containing all Relationships which
   * which pass the specified UnaryPredicate
   **/
  public synchronized Collection getMatchingRelationships(UnaryPredicate predicate) {
    return filter(predicate);
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role.
   * 
   * @param role Role to look for
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role
   **/
  public synchronized Collection getMatchingRelationships(final Role role) {
    final RelationshipScheduleImpl schedule = this;
    return filter(new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return schedule.getOtherRole(relationship).equals(role);
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final long startTime, 
                                                          final long endTime) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).equals(role)) &&
                (relationship.getStartTime() < endTime) &&
                (relationship.getEndTime() > startTime));
      }
    });
  }


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final TimeSpan timeSpan) {
    return getMatchingRelationships(role, timeSpan.getStartTime(),
                                    timeSpan.getEndTime());
  }

  /** getMatchingRelationships - return all Relationships which match the 
   * specified role, other object, and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role, direct object flag and overlap the 
   * specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final HasRelationships other,
                                                          final long startTime, 
                                                          final long endTime) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).equals(role)) &&
                (schedule.getOther(relationship).equals(other)) &&
                ((relationship.getStartTime() < endTime) &&
                 (relationship.getEndTime() > startTime)));
      }
    });
  }



  /** getMatchingRelationships - return all Relationships which match the 
   * specified role, direct object flag, and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final Role role, 
                                                          final HasRelationships other,
                                                          final TimeSpan timeSpan) {
    return getMatchingRelationships(role,
                                    other,
                                    timeSpan.getStartTime(), 
                                    timeSpan.getEndTime());
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role, direct object flag and overlap the 
   * specified time span
   **/
  public synchronized Collection getMatchingRelationships(final HasRelationships other,
                                                          final long startTime, 
                                                          final long endTime) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOther(relationship).equals(other)) &&
                ((relationship.getStartTime() < endTime) &&
                 (relationship.getEndTime() > startTime)));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final HasRelationships other,
                                                          final TimeSpan timeSpan) {
    return getMatchingRelationships(other,
                                    timeSpan.getStartTime(), 
                                    timeSpan.getEndTime());
  }

  /** getMatchingRelationships - return all Relationships where the role
   * ends with the specifed suffix and overlap the specified time span.
   * 
   * @param ruleSuffix String
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified role suffix and overlap the 
   * specified time span
   **/
  public synchronized Collection getMatchingRelationships(final String roleSuffix,
                                                          final long startTime, 
                                                          final long endTime) {
    final RelationshipScheduleImpl schedule = this;
    return filter( new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((schedule.getOtherRole(relationship).getName().endsWith(roleSuffix)) &&
                ((relationship.getStartTime() < endTime) &&
                 (relationship.getEndTime() > startTime)));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships where the role
   * ends with the specifed suffix and overlap the specified time span.
   * 
   * @param ruleSuffix String
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified role suffix and overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final String roleSuffix,
                                                          final TimeSpan timeSpan) {
    return getMatchingRelationships(roleSuffix,
                                    timeSpan.getStartTime(), 
                                    timeSpan.getEndTime());
  }


  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span. 
   * 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final long startTime, 
                                                          final long endTime) {
    return filter(new UnaryPredicate() {
      public boolean execute(Object obj) {
        Relationship relationship = (Relationship)obj;
        return ((relationship.getStartTime() < endTime) &&
                (relationship.getEndTime() > startTime));
      }
    });
  }

  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span.
   * 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain overlap the specified time span
   **/
  public synchronized Collection getMatchingRelationships(final TimeSpan timeSpan) {
    return getMatchingRelationships(timeSpan.getStartTime(), 
                                    timeSpan.getEndTime());
  }

  /** getMyRole - return role for schedule's HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getMyRole(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getRoleA();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getRoleB();
    } else {
      return null;
    }
  }

  /** getMyRole - return role for other HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getOtherRole(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getRoleB();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getRoleA();
    } else {
      return null;
    }
  }

  /** getOther  - return other (i.e. not schedule's) HasRelationships in the
   * specified relationship.
   *
   * @param relationship Relationship
   * @return HasRelationships
   */
  public HasRelationships getOther(Relationship relationship) {
    if (relationship.getA().equals(getHasRelationships())) {
      return relationship.getB();
    } else if (relationship.getB().equals(getHasRelationships())) {
      return relationship.getA();
    } else {
      return null;
    }
  }

  private static class TestAsset implements org.cougaar.core.society.UniqueObject, HasRelationships {
    private RelationshipSchedule mySchedule;
    private org.cougaar.core.society.UID myUID;

    public TestAsset() {
      mySchedule = new RelationshipScheduleImpl(this);
    }
  

    public RelationshipSchedule getRelationshipSchedule() {
      return mySchedule;
    }

    public void setRelationshipSchedule(RelationshipSchedule newSchedule) {
      mySchedule = newSchedule;
    }

    public boolean isLocal() {
      return true;
    }

    public void setLocal(boolean flag) {
    }

    public boolean isSelf() {
      return isLocal();
    }

    public void setUID(org.cougaar.core.society.UID uid) {
      myUID = uid;
    }

    public org.cougaar.core.society.UID getUID() {
      return myUID;
    }
  }
  
  public static void main(String []args) {
    int uidNum = 0;
    UID uid;

    TestAsset testAsset0 = new TestAsset();
    testAsset0.setUID(new UID("testAsset",uidNum));
    uidNum++;

    TestAsset testAsset1 = new TestAsset();
    testAsset1.setUID(new UID("testAsset", uidNum));
    uidNum++;

    TestAsset testAsset2 = new TestAsset();
    testAsset2.setUID(new UID("testAsset",uidNum));
    uidNum++;

    Role.create("ParentProvider", "ParentCustomer");
    Role parent = Role.getRole("ParentProvider");
    Role child = Role.getRole("ParentCustomer");
    
    Role.create("GarbageCustomer", "GarbageProvider");

    Relationship rel1 = new RelationshipImpl(0, 10, parent, 
                                             testAsset0, testAsset1);
    Relationship rel2 = new RelationshipImpl(5, 15, parent, 
                                             testAsset1, testAsset0);
    Relationship rel3 = new RelationshipImpl(2, 9, child,
                                             testAsset1, testAsset0);
    Relationship rel4 = new RelationshipImpl(0, 30, 
                                             Role.getRole("GarbageCustomer"),
                                             testAsset0, 
                                             testAsset1);

    Relationship testRel = new RelationshipImpl(0, 30, Role.getRole("GarbageCustomer"), 
                                                testAsset1, testAsset0);

    RelationshipSchedule schedule = new RelationshipScheduleImpl(testAsset0);
    schedule.add(rel1);
    schedule.add(rel2);
    schedule.add(rel3);
    schedule.add(rel4);

    
    System.out.println(schedule);
    System.out.println(schedule.iterator());
    Collection collection = schedule.getMatchingRelationships(parent);
    Iterator iterator = collection.iterator();
    System.out.println("Role -" + parent);
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    collection = schedule.getMatchingRelationships(parent, 10, 17);
    iterator = collection.iterator();
    System.out.println("Role -" + parent + " time span 10 - 17");
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    collection = schedule.getMatchingRelationships(child, 0, 5);
    iterator = collection.iterator();
    System.out.println("Role -" + child + " time span 0 - 5"); 
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };

    collection = schedule.getMatchingRelationships("Provider", 10, 17);
    iterator = collection.iterator();
    System.out.println("Role Suffix -  'Provider',  time span 10 - 17");
    while(iterator.hasNext()) {
      System.out.println((Relationship)iterator.next());
    };
    
    schedule.add(testRel);

  }


}




