/*
 * <COPYRIGHT>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin.util;

import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.asset.AbstractAsset;

import java.util.*;

/**
 * Provides utility methods for building Expander Plugins.
 */
public class ExpanderHelper {

  /**
   * Checks if the Task is of specified OFTYPE.
   */
  public static boolean isOfType( Task t, String p, String typeid ) {
    PrepositionalPhrase pPhrase = t.getPrepositionalPhrase(p);
    if (pPhrase != null) {
      Object indirectobj = pPhrase.getIndirectObject();
      if (indirectobj instanceof AbstractAsset) {
	AbstractAsset aa = (AbstractAsset) indirectobj;
	String mytypeid = aa.getTypeIdentificationPG().getTypeIdentification();
	return (mytypeid.equals(typeid));
      }
    }
    return false;
  }

  /**
   * Takes "a" subtask, generates a workflow for that subtask. This newly created
   * Expansion is wired properly and returned.
   * @deprecated use PlugInHelper.wireExpansion(Task parent, NewTask subTask, RootFactory ldmf) instead
   */
  public static Expansion wireExpansion(Task parent, NewTask subTask, RootFactory ldmf){

    NewWorkflow wf = ldmf.newWorkflow();

    Task t = parent;

    wf.setParentTask( t );
    subTask.setWorkflow( wf );
    wf.addTask( subTask );

    //End of creating NewWorkflow. Start creating an Expansion.
    // pass in a null estimated allocationresult for now
    Expansion exp = ldmf.createExpansion( t.getPlan(),t, wf, null );

    // Set the Context of the subTask to be that of the parent, unless it has already been set
    if ((Task)subTask.getContext() == null) {
      subTask.setContext(parent.getContext());
    }

    return exp;
  }

  /**
   * Takes a Vector of subtasks, generates a workflow for those subtasks. This newly created
   * Expansion is wired properly and returned.
   * @deprecated use PlugInHelper.wireExpansion(Task parentTask, Vector subTasks, RootFactory ldmf) instead.
   */
  public static Expansion wireExpansion( Vector subTasks, RootFactory ldmf, Task parentTask, NewWorkflow wf ) {
    wf.setParentTask( parentTask );

    Context context = parentTask.getContext();
    for (Enumeration esubTasks = subTasks.elements(); esubTasks.hasMoreElements(); ) {
      Task myTask = (Task)esubTasks.nextElement();
      ((NewTask)myTask).setWorkflow( (Workflow)wf );
      wf.addTask( myTask );
      // Set the Context of the subtask if it hasn't already been set
      if (myTask.getContext() == null) {
	((NewTask)myTask).setContext(context);
      }
    }

    //End of creating NewWorkflow. Start creating an Expansion.
    // pass in a null estimated allocationresult for now
    Expansion exp = ldmf.createExpansion( parentTask.getPlan(), parentTask, (Workflow)wf, null );

    return exp;
  }

  /** Publish a new Expansion and its subtasks.
   * e.g.
   *   publishAddExpansion(getSubscriber(), myExpansion);
   * @deprecated use PlugInHelper.publishAddExpansion(Subscriber sub, PlanElement exp) instead
   **/
  public static void publishAddExpansion(Subscriber sub, PlanElement exp) {
    sub.publishAdd(exp);

    for (Enumeration esubTasks = ((Expansion)exp).getWorkflow().getTasks(); esubTasks.hasMoreElements(); ) {
      Task myTask = (Task)esubTasks.nextElement();
      sub.publishAdd(myTask);
    }
  }


  /** Takes a subscription, gets the changed list and updates the changedList.
   * @deprecated use PlugInHelper.updateAllocationResult(IncrementalSubscription sub) instead
   */
  public static void updateAllocationResult ( IncrementalSubscription sub ) {

    Enumeration changedPEs = sub.getChangedList();
    while ( changedPEs.hasMoreElements() ) {
      PlanElement pe = (PlanElement)changedPEs.nextElement();
      if (pe.getReportedResult() != null) {
        //compare entire pv arrays
        AllocationResult repar = pe.getReportedResult();
        AllocationResult estar = pe.getEstimatedResult();
        if ( (estar == null) || (!repar.isEqual(estar)) ) {
          pe.setEstimatedResult(repar);
          sub.getSubscriber().publishChange( pe, null );
        }
      }
    }
  }

    /**
     * @deprecated use PlugInHelper.createEstimatedAllocationResult(Task t, RootFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, RootFactory ldmFactory) {
      return createEstimatedAllocationResult(t, ldmFactory, 0.0);
    }
    /**
     * @deprecated use PlugInHelper.createEstimatedAllocationResult(Task t, RootFactory ldmf, double confrating, boolean success) instead
     */
    public static AllocationResult createEstimatedAllocationResult(Task t, RootFactory ldmFactory, double confrating) {
	Enumeration preferences = t.getPreferences();
        Vector aspects = new Vector();
        Vector results = new Vector();
        while (preferences != null && preferences.hasMoreElements()) {
          Preference pref = (Preference) preferences.nextElement();
          int at = pref.getAspectType();
          aspects.addElement(new Integer(at));
          ScoringFunction sf = pref.getScoringFunction();
          // allocate as if you can do it at the "Best" point
          double myresult = ((AspectScorePoint)sf.getBest()).getValue();
          results.addElement(new Double(myresult));
        }
        int[] aspectarray = new int[aspects.size()];
        double[] resultsarray = new double[results.size()];
        for (int i = 0; i < aspectarray.length; i++)
          aspectarray[i] = (int) ((Integer)aspects.elementAt(i)).intValue();
        for (int j = 0; j < resultsarray.length; j++ )
          resultsarray[j] = (double) ((Double)results.elementAt(j)).doubleValue();

        AllocationResult myestimate = ldmFactory.newAllocationResult(confrating, true, aspectarray, resultsarray);
        return myestimate;
    }
}

