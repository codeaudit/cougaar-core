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

package org.cougaar.planning.ldm.plan;

import java.util.Collection;

import org.cougaar.core.blackboard.ChangeReport;
import org.cougaar.util.TimeSpan;
import org.cougaar.util.UnaryPredicate;

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

  /** getMatchingRelationships - return all Relationships which pass the
   * specified UnaryPredicate.
   * 
   * @param predicate UnaryPredicate to use in screening Relationships
   * @return a sorted Collection containing all Relationships which
   * which pass the specified UnaryPredicate
   **/
  Collection getMatchingRelationships(UnaryPredicate predicate);

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


  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span. 
   * 
   * @param startTime long specifying the start of the time span
   * @param endTime long specifying the end of the time span
   * @return a sorted Collection containing all Relationships which
   * which match overlap the specified time span
   **/
  Collection getMatchingRelationships(long startTime,
                                      long endTime);

  /** getMatchingRelationships - return all Relationships which overlap the 
   * specified time span.
   * 
   * @param timeSpan TimeSpan 
   * @return a sorted Collection containing all Relationships which
   * which contain overlap the specified time span
   **/
  Collection getMatchingRelationships(TimeSpan timeSpan);

  /** getMyRole - return role for schedule's HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  Role getMyRole(Relationship relationship);

  /** getMyRole - return role for other HasRelationships in the specified
   * relationship.
   *
   * @param relationship Relationship
   * @return Role
   */
  Role getOtherRole(Relationship relationship);

  /** getOther  - return other (i.e. not schedule's) HasRelationships in the
   * specified relationship.
   *
   * @param relationship Relationship
   * @return HasRelationships
   */
  HasRelationships getOther(Relationship relationship);

  class RelationshipScheduleChangeReport 
    implements ChangeReport 
  {
    public RelationshipScheduleChangeReport() {
    }

    public String toString() {
      return "RelationshipScheduleChangeReport";
    }
  }

}














