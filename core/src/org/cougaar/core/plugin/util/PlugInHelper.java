package org.cougaar.core.plugin.util;

import org.cougaar.domain.planning.ldm.RootFactory;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.AspectScorePoint;
import org.cougaar.domain.planning.ldm.plan.Context;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.NewWorkflow;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.Preposition;
import org.cougaar.domain.planning.ldm.plan.PrepositionalPhrase;
import org.cougaar.domain.planning.ldm.plan.Role;
import org.cougaar.domain.planning.ldm.plan.ScoringFunction;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Workflow;

import org.cougaar.domain.planning.ldm.asset.AbstractAsset;
import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.core.cluster.Subscription;
import org.cougaar.core.cluster.Subscriber;

import java.util.Enumeration;
import java.util.Vector;


public class PlugInHelper {

    /**
     * Retuns an AllocationResult based on the preferences of Task <t>
     * and specified confidence rating <confrating> and success <success>.
     * Results are estimated to be the "best" possible based on the
     * preference scoring function. AllocationResult is null if
     * <t> has no preferences.
     */
    //force everyone to specify the confidence rating and success
    public static AllocationResult createEstimatedAllocationResult(Task t, RootFactory ldmFactory, double confrating, boolean success) {
	Enumeration preferences = t.getPreferences();
	Vector aspects = new Vector();
	Vector results = new Vector();
	while ( preferences != null && preferences.hasMoreElements() ) {
	  // do something really simple for now.
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

	AllocationResult myestimate = ldmFactory.newAllocationResult(confrating, success, aspectarray, resultsarray);
	return myestimate;
    }

    /**
     * updatePlanElement looks for differences between the reported and
     * estimated allocation results. If they are not equal (== for now)
     * then the estimated value is set to the reported
     * value. Return true if <pe> has been changed, false otherwise.
     */
    public static boolean updatePlanElement ( PlanElement pe) {
	AllocationResult repar = pe.getReportedResult();
	if (repar != null) {
	    //compare the result objects.
	    // If they are NOT equal, re-set the estimated result, return true.
	    AllocationResult estar = pe.getEstimatedResult();
	    //eventually change second comparison from == to isEqual ?
	    if (! (repar == estar) ) {
	      	pe.setEstimatedResult(repar);
		return true;
	    }
	}
	return false;
    }

    /**
     * For each PlanElement of <sub> which has changed, if the
     * estimated and reported results are different, change
     * estimated to reported and publish the PlanElement change.
     */
    public static void updateAllocationResult( IncrementalSubscription sub) {
        Enumeration changedPEs = sub.getChangedList();
        while ( changedPEs.hasMoreElements() ) {
            PlanElement pe = (PlanElement)changedPEs.nextElement();
            if( updatePlanElement(pe) ) {
                sub.getSubscriber().publishChange( pe );
            }
        }
    }

    //4 different wireExpansion methods
    //2 for single subtask, 2 for vector of subtasks
    //2 with null estimate allocation result, 2 with specified  estimated allocation result

    /**
     * Returns an expansion based on <parent> and <subTask>,
     * with appropriate relations set. Specifically,
     * puts the subtask in a NewWorkflow, sets the Workflow's
     * parent task to <parent> and sets the subtask Workflow.
     * Sets the subtask to be removed if the Workflow is removed.
     * If <subTask> has no context, sets it to that of <parent>
     * Uses a null estimated AllocationResult for the expansion.
     */
    public static Expansion wireExpansion(Task parent, NewTask subTask, RootFactory ldmf){
	//use a null estimated allocation result
	return wireExpansion(parent, subTask, ldmf, null);
    }

    /**
     * Same as wireExpansion(Task, NewTask, RootFactory) but uses the
     * specified AllocationResult for the expansion.
     */
    public static Expansion wireExpansion(Task parent, NewTask subTask, RootFactory ldmf, AllocationResult ar){

	NewWorkflow wf = ldmf.newWorkflow();

	wf.setParentTask( parent );
	subTask.setWorkflow( wf );
        subTask.setParentTask(parent);
	wf.addTask( subTask );

    // Set the Context of the subTask to be that of the parent, unless it has already been set
	if (subTask.getContext() == null) {
	    subTask.setContext(parent.getContext());
	}

	//End of creating NewWorkflow. Start creating an Expansion.
	Expansion exp = ldmf.createExpansion( parent.getPlan(), parent, wf, ar );

	return exp;
    }

    /**
     * Wire a new subtask into an existing expansion
     **/
    public static void wireExpansion(Expansion exp, NewTask subTask){
        Task parent = exp.getTask();
        NewWorkflow wf = (NewWorkflow) exp.getWorkflow();
        subTask.setParentTask(parent);
	subTask.setWorkflow(wf);
	wf.addTask(subTask);

        // Set the Context of the subTask to be that of the parent, unless it has already been set
	if ( subTask.getContext() == null) {
	    subTask.setContext(parent.getContext());
	}
    }

  /**
   * Same as wireExpansion(Task, NewTask, RootFactory) except that a Vector
   * of subtasks is used. All the subtasks in the Vector are added to the
   * Workflow.
   */
    public static Expansion wireExpansion( Task parentTask, Vector subTasks, RootFactory ldmf ) {
	return wireExpansion( parentTask, subTasks, ldmf, null);
    }

    /**
     * Same as wireExpansion(Task, Vector, RootFactory) except uses
     * the specified AllocationResult
     */
    public static Expansion wireExpansion( Task parentTask, Vector subTasks, RootFactory ldmf, AllocationResult ar ) {

	NewWorkflow wf = ldmf.newWorkflow();

	wf.setParentTask( parentTask );

	Context context = parentTask.getContext();
	for (Enumeration esubTasks = subTasks.elements(); esubTasks.hasMoreElements(); ) {
	    NewTask myTask = (NewTask) esubTasks.nextElement();
	    myTask.setWorkflow(wf);
	    myTask.setParentTask(parentTask);
	    wf.addTask(myTask);
	    // Set the Context of the subtask if it hasn't already been set
	    if (myTask.getContext() == null) {
		myTask.setContext(context);
	    }
	}

	Expansion exp = ldmf.createExpansion(parentTask.getPlan(), parentTask, wf, ar);

	return exp;
    }

    /**
     * Returns a NewTask based on <task>. The NewTask
     * has identical Verb, DirectObject, Plan, Preferences,
     * Context, and PrepositionalPhrases as <task>.
     */
    public static NewTask makeSubtask( Task task, RootFactory rtFactory ) {

	NewTask subtask = rtFactory.newTask();

	// Create copy of parent Task
	subtask.setParentTask(task);
	subtask.setDirectObject( task.getDirectObject() );
	subtask.setPrepositionalPhrases( task.getPrepositionalPhrases() );
	subtask.setVerb( task.getVerb() );
	subtask.setPlan( task.getPlan() );
	subtask.setPreferences( task.getPreferences() );
        subtask.setContext( task.getContext());

	return subtask;
    }

    /** Publish a new Expansion and its subtasks **/
  public static void publishAddExpansion(Subscriber sub, Expansion exp) {
    sub.publishAdd(exp);

    for (Enumeration esubTasks = exp.getWorkflow().getTasks(); esubTasks.hasMoreElements(); ) {
      Task myTask = (Task) esubTasks.nextElement();
      sub.publishAdd(myTask);
    }
  }

}



