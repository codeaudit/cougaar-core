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

import java.util.Enumeration;
import java.util.Date;

import org.cougaar.core.society.UID;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.Priority;

import org.cougaar.core.plugin.Annotation;

/** NewTask Interface
 * Provide setters for building Task objects.
 * Task is the essential "execute" directive,
 * instructing a subordinate or service provider
 * to plan and eventually accomplish a task.
 * A the general form of a task is:
 * Verb <DirectObject> {PrepositionalPhrase} per <Schedule> per <Constraints>
 *
 **/
	
public interface NewTask extends Task, NewDirective
{
		
		
  void setParentTask(Task pt);

  /** 
   * Sets the base or parent task of
   * a given task, where the given task is
   * an expansion of the base task. The
   * parent task could be "move vehicles
   * from point a to point b ...". An
   * expanded task could be "fuel vehicles ...".
   * @param pt - Task that is the "parenttask"
   **/

  void setParentTaskUID(UID ptuid);
		
  /** Set the Workflow that the task is a member of.
   * All Tasks are members of
   * a Workflow. The tasks that are expansions
   * of a basetask are placed in one workflow. 
   * For example, the fueltask will be a member
   * of a workflow that contains all of the tasks
   * and constraints needed to complete the basetask.
   * @param aWorkflow - The Workflow of the Task.   
   **/
			
  void setWorkflow(Workflow aWorkflow);
		
  /** 
   * Sets the prepositional phrases of the Task.  
   * A PrepositionalPhrase object contains a String
   * representation of the preposition (from, to, with, etc.) 
   * and an object representing the indirect object. The indirect
   * object can be an Asset (or AssetGroup), a Location (or two) or
   * a Capabiltiy.
   * For example, in the task
   * "UnitA requisitions commodityB from UnitC"...
   * the PrepositionalPhrase is "from UnitC".
   * @param enumOfPrepPhrase - The Prep Phrases of the Task.
   **/
			
  void setPrepositionalPhrases(Enumeration enumOfPrepPhrase);

		
  /** 
   * Sets the prepositional phrase (note singularity) of the Task.  
   * A PrepositionalPhrase object contains a String
   * representation of the preposition (from, to, with, etc.) 
   * and an object representing the indirect object. The indirect
   * object can be an Asset (or AssetGroup), a Location (or two) or
   * a Capabiltiy.
   * For example, in the task
   * "UnitA requisitions commodityB from UnitC"...
   * the PrepositionalPhrase is "from UnitC".
   * @param aPrepPhrase - The Prep Phrase of the Task.
   **/
			
  void setPrepositionalPhrase(PrepositionalPhrase aPrepPhrase);
		
  /**
   * The setVerb method sets the verb of the Task.
   * For example, in the Task "fuel vehicles...", the
   * Verb is the object represented by "fuel".
   * @param aVerb - The verb of the Task. 
   **/
			
  void setVerb(Verb aVerb);
  
 /**
 * Sets the Asset (or AssetGroup) that is being acted upon
 * by the Task.  For example, in the task "fuel
 * vehicle 14 ..." the direct object is "vehicle 14".
 * @param dobj - The DirectObject of the Task.
 **/
  void setDirectObject(Asset dobj);
		
  /** 
   * @param aPlan - The Plan for which this task is a part of.
   **/
			
  void setPlan(Plan aPlan);
  
  /** set the preferences on this task.
   * Implicit collection of generic Task.PreferenceChangeReport instances.
   * Implicit collection of detailed ChangeReports without old value.
   * @param thepreferences
   */
  void setPreferences(Enumeration thepreferences);
  
  /** Set just one preference in the task's preference list.
   * Implicit collection of detailed Task.PreferenceChangeReport instances.
   **/
  void setPreference(Preference thePreference);


  /** alias for setPreference.  Multiple preferences of the same type are
   * not allowed so setPreference and addPreference are equivalent.
   **/
  void addPreference(Preference aPreference);
  
  /** Set the priority of this task.
    * Note that this should only be used when there are competing tasks
    * from the SAME customer.
    * @param thepriority
    * @see org.cougaar.domain.planning.ldm.plan.Priority
    */
  void setPriority(byte thepriority);
  
  /** Set the Commitment date of this task.
    * After this date, the task is not allowed to be rescinded
    * or re-planned (change in preferences).
    * @param commitDate
    */
  void setCommitmentDate(Date commitDate);

  /**
   * Set the deleted status of this task
   **/
  void setDeleted(boolean newDeleted);

  /**
   * Add to the collection of observable aspect types
   **/
  void addObservableAspect(int aspectType);

  /** Set the collection of AuxiliaryQueryTypes that the task is
    * requesting information on.  This information will be returned in
    * the AllocationResult of this task's disposition.
    * Note that this method clears all previous types.
    * @param int[]  A collection of defined AuxiliaryQueryTypes
    * @see org.cougaar.domain.planning.ldm.plan.AuxiliaryQueryType
    */
  void setAuxiliaryQueryTypes(int[] thetypes);

  /**
   * Set the problem Context of this task.
   * @see org.cougaar.domain.planning.ldm.plan.Context
   **/
  void setContext(Context context);
}
