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

import java.util.Collection;

import org.cougaar.core.cluster.ChangeReport;
import org.cougaar.util.TimeSpan;

/** 
 * A RelationshipSchedule is a representation of an object (must implement
 * HasRelationships) relationships 
 **/

public interface RelationshipSchedule extends Schedule {

  /**
   * @return HasRelationships The object whose relationships are contained in
   * the schedule
   */
  HasRelationships getHasRelationships();

  /** getMatchingRelationships - return all Relationships where the other 
   * has the specified role. getMatchingRelationships(SUBORDINATE) returns 
   * relationships with my subordinates
   * 
   * @param role Role to look for
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role
   **/
  Collection getMatchingRelationships(Role role);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(Role role, long startTime, long endTime);


  /** getMatchingRelationships - return all Relationships which contain the
   * specified role and overlap the specified time span.
   * 
   * @param role Role to look for
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(Role role, TimeSpan timeSpan);


  /** getMatchingRelationships - return all Relationships which contain the 
   * specified other object, match the specified role, and overlap the 
   * specified time span.
   *
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which contain 
   * the specified other object, match the specified role and direct object 
   * flag, and overlap the specified time span.
   **/
  Collection getMatchingRelationships(Role role, 
                                      HasRelationships otherObject,
                                      long startTime, long endTime);

  /** getMatchingRelationships - return all Relationships which contain the 
   * specified other object, match the specified role, and overlap the 
   * specified time span.
   * 
   * @param role Role to look for
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(Role role,
                                      HasRelationships otherObject,
                                      TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param otherObject HasRelationships 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role and overlap the specified time span
   **/
  Collection getMatchingRelationships(HasRelationships otherObject,
                                      long startTime,
                                      long endTime);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * 
   * @param otherObject HasRelationships 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified other HasRelationships and overlap the 
   * specified time span
   **/
  Collection getMatchingRelationships(HasRelationships otherObject,
                                      TimeSpan timeSpan);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified role suffix andd overlap the specified time span. 
   * getMatchingRelationships("Provider", startTime, endTime) will return
   * relationships with providers.
   * 
   * @param roleSuffix String specifying the role suffix to match
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match the specified Role suffix and overlap the specified time span
   **/
  Collection getMatchingRelationships(String roleSuffix,
                                      long startTime,
                                      long endTime);

  /** getMatchingRelationships - return all Relationships which contain the
   * specified other object and overlap the specified time span.
   * getMatchingRelationships("Provider", timeSpan) will return
   * relationships with providers.
   * 
   * @param roleSuffix String specifying the role suffix to match
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain the specified role suffix and overlap the 
   * specified time span
   **/
  Collection getMatchingRelationships(String roleSuffix,
                                      TimeSpan timeSpan);

  /** getMyRole - return role for schedule's HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getMyRole(Relationship relationship);

  /** getMyRole - return role for other HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  public Role getOtherRole(Relationship relationship);

  /** getOther  - return other (i.e. not schedule's) HasRelationships in the
   * specified relationship.
   *
   * @param relationship Relationship
   * @return HasRelationships
   */
  public HasRelationships getOther(Relationship relationship);

  public static class RelationshipScheduleChangeReport implements 
    ChangeReport {
    public RelationshipScheduleChangeReport() {
    }

    public String toString() {
      return "RelationshipScheduleChangeReport";
    }
  }

}














