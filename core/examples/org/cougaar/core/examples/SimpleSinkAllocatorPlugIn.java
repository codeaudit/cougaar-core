package org.cougaar.util;

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.PlanElementSet; 
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Date;

// A simple plugin to respond with any task with an allocation result
// satisfying the preferences.
public class SimpleSinkAllocatorPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
  // Subscription for all tasks
  private IncrementalSubscription allTasks;
  private UnaryPredicate allTasksPredicate = new UnaryPredicate() {
    public boolean execute(Object o) { 
      return o instanceof Task;
    }};

  // Plan Element set for all tasks predicate
  private PlanElementSet allTaskPEs;

  // Single asset to which to allocate all tasks
  private Asset sink_asset;

  // Subscribe to all tasks, and create a dummy asset to which to allocate
  // everything
  public void setupSubscriptions()
  {

    // Subscribe for all tasks
    allTasks = 
      (IncrementalSubscription)subscribe(allTasksPredicate, allTaskPEs);

    // Create an abstract asset to which to allocate everything
    sink_asset = theLDMF.createPrototype("AbstractAsset", "SinkAsset");
    publishAdd(sink_asset);
  }

  // Allocate every new task, and reallocate every changed task
  public void execute()
  {

    // Allocate every new task
    for(Enumeration e_added = allTasks.getAddedList();
	e_added.hasMoreElements();)
      {
	Task task = (Task)e_added.nextElement();

	// Compute allocation result indicating success within preferences
	AllocationResult result = computeAllocationResult(task);

	// Generate allocation
	Allocation allocation = 
	  theLDMF.createAllocation(task.getPlan(), task,
				   sink_asset, result, Role.AVAILABLE);

	// publish new allocation
	publishAdd(allocation);
      }

    // Change allocation result on every new task
    for(Enumeration e_changed = allTasks.getChangedList();
	e_changed.hasMoreElements();)
      {
	Task task = (Task)e_changed.nextElement();
	// Compute new allocation result for changed task
	AllocationResult result = computeAllocationResult(task);   
   
	// Find PE (allocation) for task (don't use getPlanElement)
	Allocation allocation = (Allocation)allTaskPEs.findPlanElement(task);

	if (allocation != null) {

	  // Set the new estimated result for the allocation based on changed
	  // preferences
	  allocation.setEstimatedResult(result);
	  publishChange(allocation);

	} else {
	  System.out.println("Error! Should have a plan element for a changed task allocation!");
	}
      }
  }

  private AllocationResult computeAllocationResult(Task task) 
  {
    System.out.print("[" + getClusterIdentifier() + "] allocating task " + 
		       task.getUID() + "[" + task.getVerb() + "] :");

    // Grab the preferences and grab the aspect types and results
    // Don't know how big the list is, so store in an array and then convert
    Enumeration all_preferences = task.getPreferences();
    Vector all_aspect_types = new Vector();
    Vector all_aspect_results = new Vector();
    while(all_preferences.hasMoreElements()) {
      // Grab each preference and save aspect type and result
      Preference preference = (Preference)all_preferences.nextElement();
      int aspect_type = preference.getAspectType();
      double preference_result = computePreferenceResult(preference);
      String preference_result_image = Double.toString(preference_result);
      if ((aspect_type == AspectType.START_TIME) ||
	  (aspect_type == AspectType.END_TIME)) {
	preference_result_image = new Date((long)preference_result).toString();
      }
      System.out.print(" [" + aspect_type + "] : " + preference_result_image);
      all_aspect_types.addElement(new Integer(aspect_type));
      all_aspect_results.addElement(new Double(preference_result));
    }
    System.out.println("");

    // Copy the Integer/Double values 
    // into aspect_type(int)/aspect_result(double) arrays
    int []aspect_types = new int[all_aspect_types.size()];
    for(int i = 0; i < aspect_types.length; i++) {
      aspect_types[i] = 
	((Integer)(all_aspect_types.elementAt(i))).intValue();
    }

    double []aspect_results = new double[all_aspect_results.size()];
    for(int i = 0; i < aspect_results.length; i++) {
      aspect_results[i] = 
	((Double)(all_aspect_results.elementAt(i))).doubleValue();
    }
      
    // Compute new allocation result
    AllocationResult result = 
      theLDMF.newAllocationResult(1.0, // rating,
				  true, // success,
				  aspect_types,
				  aspect_results);

    // Add in any auxiliary queries
    int []query_types = task.getAuxiliaryQueryTypes();
    for(int i = 0; i < query_types.length; i++) {
      int query_type = query_types[i];
      if (query_type >= 0)
	result.addAuxiliaryQueryInfo(query_type, "QueryResponse-" + query_type);
    }

    return result;
  }

  // Compute preference result for given preference based on optimal point
  // in scoring function
  private double computePreferenceResult(Preference preference)
  {
    ScoringFunction func = preference.getScoringFunction();
    AspectScorePoint point = func.getBest();
    return point.getValue();
  }

}
