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

import org.cougaar.core.blackboard.Publishable;

import org.cougaar.core.blackboard.ChangeReport;

import java.util.*;

import org.cougaar.core.agent.*;
import org.cougaar.planning.ldm.asset.Asset;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.util.UID;

import org.cougaar.core.plugin.Annotation;

/** Task Interface
  * Task is the essential "execute" directive,
  * instructing a subordinate or service provider
  * to plan and eventually accomplish a task.
  * A the general form of a task is:
  * Verb <DirectObject> {PrepositionalPhrase} per <Schedule> per <Constraints>
  **/
	
public interface Task
  extends Directive, UniqueObject, Priority, Annotatable, Publishable
{
		
  /** 
   * Returns the UID of the base or parent task of
   * a given task, where the given task is
   * an expansion of the base task. The
   * parent task could be "move vehicles
   * from point a to point b ...". An
   * expanded task could be "fuel vehicles ...".
   * </PRE> UID basetask = fueltask.getParentTaskUID(); </PRE>
   * @return UID of the Task that is the "parenttask"
   **/
		
  UID getParentTaskUID();
		
  /** 
   * All Tasks are members of
   * a Workflow. The tasks that are expansions
   * of a basetask are placed in one workflow. 
   * For example, the fueltask will be a member
   * of a workflow that contains all of the tasks
   * and constraints needed to complete the basetask.
   * <PRE> Workflow myworkflow = fueltask.getWorkflow(); </PRE>
   * @return Workflow  Returns the Workflow that the task is a member of. 
   **/
			
  Workflow getWorkflow();
		
  /** 
   * Returns the prepositional phrase(s) of the Task.  
   * A PrepositionalPhrase object contains a String
   * representation of the preposition (from, to, with, etc.) 
   * and an object representing the indirect object. The indirect
   * object will be an Asset which can represent an Asset, AssetGroup or Location.
   * For example, in the task
   * "UnitA requisitions commodityB from UnitC"...
   * the PrepositionalPhrase is "from UnitC".
   * @return An enumeration of PrepositionalPhrases
   * @see Preposition
   **/
			
  Enumeration getPrepositionalPhrases();

		
  /**
   * Return the first PrepositionalPhrase found with the
   * specified Preposition.  Returns null if not found.
   * @param preposition One of the strings named in 
   * org.cougaar.planning.ldm.plan.Preposition.
   **/
  PrepositionalPhrase getPrepositionalPhrase(String preposition);


  /**
   * The getVerb method returns the verb of the Task.
   * For example, in the Task "fuel vehicles...", the
   * Verb is the object represented by "fuel".
   * <PRE> Verb mytaskverb = fueltask.getVerb(); </PRE>
   * @return the Verb of the Task.
   **/
			
  Verb getVerb();
  
  /**
  * Returns the Asset (or AssetGroup) that is being acted upon
  * by the Task.  For example, in the task "fuel
  * vehicle 14 ..." the direct object is "vehicle 14".
  * @return the Direct Object of the task.
  **/
  Asset getDirectObject();
		
  /** 
   * @return Plan The Plan for which this task is a part of.
   **/
			
  Plan getPlan();
		
  /**
   * Returns PlanElement that this Task is associated with.  
   * Can be used to discern between expandable and non-expandable
   * Tasks.  If Task has no PlanElement associated with it, will 
   * return null.
   */
  PlanElement getPlanElement();
  
  /** get the preferences on this task.
    * @return Enumeration{Preference}
    */
  Enumeration getPreferences();
  
  /** return the preference for the given aspect type
    * will return null if there is not a preference defined for this aspect type
    * @param aspect_type The Aspect referenced by the preference
    */
  Preference getPreference(int aspect_type);
  
  /** return the preferred value for a given aspect type
    * from the defined preference (and scoring function)
    * will return -1 if there is not a preference defined for this aspect type
    * @param aspect_type The Aspect referenced by the preference
    */
  double getPreferredValue(int aspect_type);
  
  /** Get the priority of this task.
    * Note that this should only be used when there are competing tasks
    * from the SAME customer.
    * @return  The priority of this task
    * @see org.cougaar.planning.ldm.plan.Priority
    */
  byte getPriority();
  
  /** WARNING: This method may return null if the commitment date is undefined
    * Get the Commitment date of this task.
    * After this date, the task is not allowed to be rescinded
    * or re-planned (change in preferences).
    */
  Date getCommitmentDate();

  /**
   * Get the deleted status of this task.
   **/
  boolean isDeleted();

  Enumeration getObservableAspects();

  /** 
    * Check to see if the current time is before the Commitment date.
    * Will return true if we have not reached the commitment date.
    * Will return true if the commitment date is undefined.
    * Will return false if we have passed the commitment date.
    * @param currentdate  The current date.
    */
  boolean beforeCommitment(Date currentdate);
  
  /** Get a collection of the requested AuxiliaryQueryTypes (int).
    * Note:  if there are no types set, this will return an
    * array with one element = -1
    * @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
    */
  int[] getAuxiliaryQueryTypes();
    
  /**
   * Get the problem Context (if any) for this task.
   * @see Context
   **/
  Context getContext();

  interface TaskChangeReport extends ChangeReport {}

  class PreferenceChangeReport implements TaskChangeReport {
    private int type;
    public final static int UNDEFINED_TYPE = AspectType.UNDEFINED;
    private Preference old = null;

    public PreferenceChangeReport() {
      type = UNDEFINED_TYPE;
    }
    public PreferenceChangeReport(int t) {
      type=t;
    }
    public PreferenceChangeReport(int t, Preference o) {
      type=t;
      old = o;
    }
    public PreferenceChangeReport(Preference o) {
      type = o.getAspectType();
      old = o;
    }
    /** May return AspectType.UNDEFINED if the aspect type id is unknown **/
    public int getAspectType() { return type; }
    public int hashCode() { return getClass().hashCode()+type; }
    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         ((PreferenceChangeReport)o).type == type);
    }
    public String toString() {
      if (type == UNDEFINED_TYPE) {
        return "PreferenceChangeReport (?)";
      } else {
        return "PreferenceChangeReport ("+type+")";
      }
    }
  }

  class PrepositionChangeReport implements TaskChangeReport {
    private String prep;

    public PrepositionChangeReport() {
      prep = null;
    }
    public PrepositionChangeReport(String p) {
      prep = p;
    }

    /** May return null if unknown **/
    public String getPreposition() { return prep; }

    public int hashCode() { 
      int hc = getClass().hashCode();
      if (prep != null) hc +=prep.hashCode();
      return hc;
    }

    public boolean equals(Object o) {
      if (o == null) return false;

      return (this == o) ||
        (o.getClass() == getClass() &&
         prep != null &&
         prep.equals(((PrepositionChangeReport)o).prep));
    }
    public String toString() {
      if (prep == null) {
        return "PrepositionChangeReport (?)";
      } else {
        return "PrepositionChangeReport ("+prep+")";
      }
    }
  }


}
