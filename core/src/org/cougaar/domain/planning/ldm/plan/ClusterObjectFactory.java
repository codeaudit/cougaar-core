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

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.measure.Latitude;
import org.cougaar.domain.planning.ldm.measure.Longitude;
import org.cougaar.domain.planning.ldm.policy.Policy;
import org.cougaar.core.society.UID;
import org.cougaar.util.TimeSpan;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * Top-level Factory for Cluster Objects which can be created by PlugIns
 * of various sorts.
 * While we do not subset these interfaces at this time, one should
 * keep in mind that a given component may choose to both 
 * restrict access to (by throwing exceptions) these accessors and to 
 * choose how these objects actually get realized.
 *
 * The majority of the factory methods are of the form <em> NewFoo newFoo() </em> 
 * where <em> Foo </em> is the basic type to be constructed.  The
 * Return value is always an Interface type which extends the basic
 * type with set-methods and other methods useful for constructing
 * objects of this basic type.
 *
 * There a few factory methods that return a completed or built object.
 * These methods usually take arguments that are used to build the object.
 * The Interface return type are usually not the extending interfaces which
 * contain set-methods (since the object has already been built).
 *
 * @author  ALPINE <alpine-software@bbn.com>
 *
 *
 *
 * @see org.cougaar.component.ComponentServesPlugIn
 */
public interface ClusterObjectFactory {
	
  NewAlert newAlert();
  NewAlertParameter newAlertParameter();
  NewAssetAssignment newAssetAssignment();
  NewComposition newComposition();
  NewConstraint newConstraint();

  NewAssignedRelationshipElement newAssignedRelationshipElement();
  NewAssignedRelationshipElement newAssignedRelationshipElement(Asset assetA,
                                                                Role roleA,
                                                                Asset assetB,
                                                                long startTime,
                                                                long endTime);
  NewAssignedRelationshipElement newAssignedRelationshipElement(Relationship relationship);

  NewAssignedAvailabilityElement newAssignedAvailabilityElement();
  NewAssignedAvailabilityElement newAssignedAvailabilityElement(Asset assignee,
                                                                long startTime,
                                                                long endTime);

  NewItineraryElement newItineraryElement();
  NewLocationRangeScheduleElement newLocationRangeScheduleElement();
  NewLocationScheduleElement newLocationScheduleElement();
  NewMPTask newMPTask();
  NewNotification newNotification();
  NewDeletion newDeletion();
  NewAssetVerification newAssetVerification();
  NewAssetVerification newAssetVerification(Asset asset, Asset assignee, Schedule schedule);
  Policy newPolicy(String policyType);
  NewPrepositionalPhrase newPrepositionalPhrase();
  NewReport newReport();
  NewScheduleElement newScheduleElement(Date start, Date end); 
  NewTask newTask();
  NewTransferableAssignment newTransferableAssignment();
  NewTransferableTransfer newTransferableTransfer();
  NewTransferableRescind newTransferableRescind();
  NewTransferableVerification newTransferableVerification(Transferable t);
  NewWorkflow newWorkflow();

 
  /** Low-level task duplication.  Tasks created by this method are
   * not suitable for use as LogPlan Objects.  This method should
   * only be called by networking infrastructure.
   **/
  NewTask shadowTask(Task t);

  //
  // Methods that return built or complete objects
  //
  

  /** Build a new TransferableTransfer
    * @param aTransferable
    * @param anAsset - should be of type Organization
    * @return TranserableTransfer
    */ 
  TransferableTransfer createTransferableTransfer(Transferable aTransferable, Asset anAsset);
  
  /** Build a new AssetTransfer (planelement)
   * @param aPlan The Plan this PlanElement is against
   * @param aTask  The Task being disposed
   * @param anAsset  The Asset that is being transferred
   * @param aSchedule  The Schedule that represents the time window the Asset will be transferred 
   * for
   * @param toAsset The Asset to which the transferred Asset is being 
   * transferred. Must have a ClusterPG
   * @param estimatedresult - allowed to be null
   * @param aRole  The Role of the Asset while transferred
   * @return AssetTransfer
   **/
  AssetTransfer createAssetTransfer(Plan aPlan, 
                                    Task aTask, 
                                    org.cougaar.domain.planning.ldm.asset.Asset anAsset, 
                                    Schedule aSchedule, 
                                    org.cougaar.domain.planning.ldm.asset.Asset toAsset, 
                                    AllocationResult estimatedresult, 
                                    Role aRole);

  /** Build a new Allocation (planelement)
   * @param aPlan - The Plan this PlanElement is against
   * @param aTask - The Task being disposed
   * @param anAsset - The Asset that the task is being assigned to.
   * @param estimatedresult - allowed to be null
   * @param aRole - The role of the Asset while performing this Task.
   * @see org.cougaar.domain.planning.ldm.Asset
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   **/
  Allocation createAllocation(Plan aPlan, 
                              Task aTask, 
                              org.cougaar.domain.planning.ldm.asset.Asset anAsset,
                              AllocationResult estimatedresult, 
                              Role aRole);
                              
  /** Build a new Expansion (planelement)
   * @param aPlan
   * @param aTask
   * @param aWorkflow
   * @param estimatedresult -allowed to be null
   * @return Expansion
   * @see org.cougaar.domain.planning.ldm.plan.Workflow
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   */
  Expansion createExpansion(Plan aPlan, Task aTask, Workflow aWorkflow, AllocationResult estimatedresult);
   
  /** Build a new Aggregation (planelement)
   * @param aPlan
   * @param aTask
   * @param aComposition
   * @param estimatedresult -allowed to be null
   * @return Aggregation
   * @see org.cougaar.domain.planning.ldm.plan.Composition
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   */

  Aggregation createAggregation(Plan aPlan, Task aTask, Composition aComposition, AllocationResult estimatedresult);
  
  /** Build a new FailedDisposition (planelement)
   * @param aPlan
   * @param aTask
   * @param failure
   * @return a Failed Disposition
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   */

  Disposition createFailedDisposition(Plan aPlan, Task aTask, AllocationResult failure);
  	
  /** Build a new Disposition
   * @param aPlan
   * @param aTask
   * @param failure
   * @return a Disposition
   * @see org.cougaar.domain.planning.ldm.plan.AllocationResult
   */
  Disposition createDisposition(Plan aPlan, Task aTask, AllocationResult result);

  /** Build a new simple schedule that contains only one start and end
   * date.
   * @param start - The start Date of the schedule
   * @param end - the end or finish Date of the schedule
   * @return Schedule - a schedule object containing one scheduleelement
   * @see java.util.Date
   **/
  NewSchedule newSimpleSchedule(Date start, Date end);

  /** Build a new simple schedule that contains only one start and end
   * date.
   * @param startTime - The start time of the schedule
   * @param endTime - the end or finish time of the schedule
   * @return Schedule - a schedule object containing one scheduleelement
   * @see java.util.Date
   **/
  NewSchedule newSimpleSchedule(long startTime, long endTime);
  
  /** Create an assigned relationship schedule.  This schedule is a container
   * of AssignedRelationshipElements. Should only be used by logic providers
   * in handling new/modified/removed AssetTransfers.
   * @param Enumeration{AssignedRelationshipElement}
   * @see org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement
   **/
  NewSchedule newAssignedRelationshipSchedule(Enumeration elements);

  /** Create an empty assigned relationship schedule. Schedule elements added
   * later must be AssignedRelationshipElements. Should only be used by logic 
   * providers in handling new/modified/removed AssetTransfers.
   * @see org.cougaar.domain.planning.ldm.plan.AssignedRelationshipElement
   **/
  NewSchedule newAssignedRelationshipSchedule();

  /** Create a location schedule.  This schedule is a container of 
   * LocationScheduleElements.
   * @param Enumeration{LocationScheduleElement}
   * @see org.cougaar.domain.planning.ldm.plan.LocationScheduleElement
   **/
  NewSchedule newLocationSchedule(Enumeration locationElements);
  
  /** Create a location range schedule.  This schedule is a container
   * of LocationRangeScheduleElements.
   * @param Enumeration{LocationRangeScheduleElement}
   * @see org.cougaar.domain.planning.ldm.plan.LocationRangeScheduleElement
   **/
  NewSchedule newLocationRangeSchedule(Enumeration locationRangeElements);
  
  /** Create a schedule that contains different types of scheduleelements.
   * Note that ScheduleElement has multiple subclasses which are excepted.
   * @param Enumeration{ScheduleElement}
   * @see org.cougaar.domain.planning.ldm.plan.ScheduleElement
   **/
  NewSchedule newSchedule(Enumeration scheduleElements);

  /**
   * newRelationship - returns a Relationship
   *
   * @param role1 Role for object1
   * @param object1 HasRelationships which has role1
   * @param object2 HasRelationships which is the other half of the 
   * relationship, role set at role1.getConverse()
   * @param timeSpan TimeSpan for which the relationship is valid
   */
  Relationship newRelationship(Role role1, HasRelationships object1,
                               HasRelationships object2,
                               TimeSpan timeSpan);
  
  /**
   * newRelationship - returns a Relationship
   *
   * @param role1 Role for object1
   * @param object1 HasRelationships which has role1
   * @param object2 HasRelationships which is the other half of the 
   * relationship, role set at role1.getConverse()
   * @param startTime long which specifies the start of the relationship 
   * @param endTime long which specifies the end of the relationship. 
   * @see org.cougaar.util.TimeSpan
   */
  Relationship newRelationship(Role role1, HasRelationships object1,
                               HasRelationships object2,
                               long startTime,
                               long endTime);
  
  /**
   * newRelationship - returns a Relationship based on the specified
   * AssignedRelationshipElement. Specified assets must match the 
   * item identifications in the AssignedRelationshipElement
   *
   * @param assignedRelationship AssignedRelationshipElement
   * @param asset1 Asset to be used in converting the 
   * AssignedRelationshipElement
   * @param asset2 Asset other asset to be used in converting the 
   * AssignedRelationshipElement
   */
  Relationship newRelationship(AssignedRelationshipElement assignedRelationship,
                               Asset asset1,
                               Asset asset2);
  
  
  /** Build an empty relationship schedule for the specified HasRelationships.
   * @param hasRelationships HasRelationships to which the relationship 
   * schedule will apply
   **/
  NewRelationshipSchedule newRelationshipSchedule(HasRelationships hasRelationships);
  
  /** Build a new relationship schedule for the specified HasRelationships.
   * @param hasRelationships HasRelationships to which the relationship 
   * schedule applies. N.B. hasRelationships must be included in all the 
   * Relationships on the schedule.
   * @param relationships Collection of Relationships for the specified 
   * HasRelationships.
   **/
  NewRelationshipSchedule newRelationshipSchedule(HasRelationships hasRelationships, 
                                                  Collection relationships);
  
  /**@param v - Pass a valid string representation of the Verb
   * @return Verb
   * @see org.cougaar.domain.planning.ldm.plan.Verb for a list of valid values
   * @deprecated use Verb constructor.
   **/
  Verb getVerb(String v);
  
  /** Build a TaskRescind Message.  This message is only sent by
   *	CCRescind - NOT PLUGINS!!!.
   *	@param task - The Task to be rescinded
   *	@param destination - The Cluster to send the TaskRescind Message to.
   *	@return TaskRescind
   **/
  TaskRescind newTaskRescind(Task task, ClusterIdentifier destination);

  /** Build a TaskRescind Message.  This message is only sent by
   *	CCRescind - NOT PLUGINS!!!.
   *	@param taskUID - The UID of the Task to be rescinded
   *	@param destination - The Cluster to send the TaskRescind Message to.
   *	@return TaskRescind
   **/
  TaskRescind newTaskRescind(UID taskUID, ClusterIdentifier destination);
  
  /** Build an AssetRescind Message.  This message is only sent by
   *	CCRescind - NOT PLUGINS!!!.
   *	@param asset - The Asset to be rescinded
   *    @param rescindeeAsset - Asset from which asset will be rescinded 
   *    @param rescindSchedule - Schedule for which the asset is rescinded
   *	@return AssetRescind
   **/
  AssetRescind newAssetRescind(org.cougaar.domain.planning.ldm.asset.Asset asset, 
                               org.cougaar.domain.planning.ldm.asset.Asset rescindeeAsset,
                               Schedule rescindSchedule);
  
  /** Create a new NON-PHASE AllocationResult
    * @param rating The confidence rating of this result.
    * @param success  Whether the allocationresult violated any preferences.
    * @param aspecttypes  The AspectTypes (and order) of the results.
    * @param result  The value for each aspect.
    * @return AllocationResult  A new AllocationResult
    */
  AllocationResult newAllocationResult(double rating, boolean success, int[] aspecttypes, double[] result);
  
  /** Create a new  PHASED AllocationResult instance. 
    * @param rating The confidence rating of this result.
    * @param success  Whether the allocationresult violated any preferences.
    * @param aspecttypes  The AspectTypes (and order) of the results.
    * @param rollup  The summary values for each aspect.
    * @param allresults  An Enumeration of Vectors representing
    * each phased collection of results.
    * For Example a phased answer may look like
    * [ [10, 100.00, c0], [5, 50.00, c3], [5, 50.00, c6] ]
    * @return AllocationResult
    */
  AllocationResult newPhasedAllocationResult(double rating, boolean success, int[] aspecttypes, double[] rollup, Enumeration allresults);
  
  /** Create a new AllocationResult that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A List of the phased results. The List should contain
   * one List of AspectValues for each phase of the results.  
   * @return AllocationResult
   */
  AllocationResult newAVPhasedAllocationResult(double rating, boolean success, AspectValue[] rollupavs, List phasedresults);
  
  /** Create a new AllocationResult that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspectvalues  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   * @return AllocationResult
   */
  AllocationResult newAVAllocationResult(double rating, boolean success, AspectValue[] aspectvalues);

  
  
  /** Create a new Preference.
    * @param aspecttype  The AspectType this preference is about.
    * @param scorefunction  The function that defines the scoring curve
    * @see org.cougaar.domain.planning.ldm.plan.AspectType
    * @see org.cougaar.domain.planning.ldm.plan.ScoringFunction
    * @return Preference
    */
  Preference newPreference(int aspecttype, ScoringFunction scorefunction);
  
  /** Create a new Preference with a weight.
    * @param aspecttype  The AspectType this preference is about.
    * @param scorefunction  The function that defines the scoring curve
    * @param aweight  The weight of the preference - must be between 0.0 - 1.0
    * @see org.cougaar.domain.planning.ldm.plan.AspectType
    * @see org.cougaar.domain.planning.ldm.plan.ScoringFunction
    * @return Preference
    */
  Preference newPreference(int aspecttype, ScoringFunction scorefunction, double aweight);
  
  /** Create a new BulkEstimate
   *  @param atask  The Task to test allocations against preference sets
   *  @param prefsets  A List containing  Preference[] represening the preference sets.
   *  @param conf  The confidence rating the allocation results should reach before returning final results
   *  @return BulkEstimate
   */
  BulkEstimate newBulkEstimate(Task atask, List prefsets, double conf);
  
  
  //
  // Special constructors
  //
  
  /** Return a reference to the the plan object which represents
   * "reality" in this cluster.
   **/
  Plan getRealityPlan();

}
