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

import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.NewExpansion;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.domain.planning.ldm.plan.NewWorkflow;
import org.cougaar.domain.planning.ldm.plan.WorkflowImpl;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.TaskScoreTable;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.SubTaskResult;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.ActiveSubscriptionObject;
import org.cougaar.core.society.UID;

import java.util.*;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
 
/** ExpansionImpl.java
 * Implementation for expansion - a form of PlanElement
 * @author ALPINE <alpine-software@bbn.com>
 * @version $Id: ExpansionImpl.java,v 1.2 2001-04-05 19:27:15 mthome Exp $
 */
 
public class ExpansionImpl extends PlanElementImpl 
  implements Expansion, NewExpansion
{
 
  private transient Workflow workflow;  // changed to transient : Persistence
  
  public ExpansionImpl() {}

  /* Constructor that assumes there is not a good estimated result at this time.
   * @param p
   * @param t
   * @param wf
   * @return Expansion
   */
  public ExpansionImpl(Plan p, Task t, Workflow wf) {
    super(p, t);
    this.workflow = wf;

    setContext();
  }
  
  /* Constructor that takes an estimated result
   * @param p
   * @param t
   * @param wf
   * @param estimatedresult
   * @return Expansion
   */
  public ExpansionImpl(Plan p, Task t, Workflow wf, AllocationResult estimatedresult) {
    super(p, t);
    workflow = wf;
    estAR = estimatedresult;
    setContext();
  }
    

  /** @return Workflow - Return the Workflow that represents the expansion of the task*/
  public Workflow getWorkflow() {
    return workflow;
  }

  public boolean removingFromLogPlan(Subscriber s) {
    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();
    Workflow w = getWorkflow();

    if (w == null) return true; // if already disconnected...

    if (w.isPropagatingToSubtasks() ) { // if we're auto-propagating
      WorkflowImpl wi = (WorkflowImpl) w;
      

      // rescind all subtasks of the workflow
      List sts = wi.clearSubTasks();    // atomic get and clear the list
      ListIterator it = sts.listIterator();
        while (it.hasNext()) {
        Task asub = (Task) it.next();
        s.publishRemove(asub);
      }
    } else {      // we're not auto-propagating
      // disconnect the WF from the parent task
      ((NewWorkflow)w).setParentTask(null);
      for (Enumeration e = w.getTasks(); e.hasMoreElements(); ) {
        NewTask wfstask = (NewTask) e.nextElement();
        wfstask.setParentTask(null);
      }
      // the plugin should reattach this workflow to a parent task. 
    }
    return true;
  }
  
  
  private transient HashMap staskinfo = new HashMap(11);
  private static final List _emptylist = Collections.unmodifiableList(new ArrayList());

  /** Called by an Expander PlugIn to get the latest copy of the allocationresults
   *  for each subtask.
   *  Information is stored in a List which contains a SubTaskResult for each subtask.  
   *  Each of the SubTaskResult objects contain the following information:
   *  Task - the subtask, boolean - whether the result changed,
   *  AllocationResult - the result used by the aggregator for this sub-task.
   *  The boolean indicates whether the AllocationResult changed since the 
   *  last time the collection was cleared by the plugin (which should be
   *  the last time the plugin looked at the list).
   *  @return List
   */
  public List getSubTaskResults() {
    synchronized (staskinfo) {
      if (staskinfo.size() == 0) {
        return _emptylist;
      } else {
        List newlist = new ArrayList(staskinfo.values()); // sigh!
        staskinfo.clear();
        return newlist;
      }
    }
  }
  
  /** WARNING CALLED BY INFRASTRUCTURE ONLY!!! **/
  public void setSubTaskResults(TaskScoreTable tst, UID changedUID) {
    if (tst == null) return;
    synchronized (staskinfo) {
      tst.fillSubTaskResults(staskinfo, changedUID);
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    synchronized (staskinfo) {
      stream.defaultWriteObject();
    }
    stream.writeObject(workflow);
  }
 

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    staskinfo = new HashMap(11);
    workflow = (Workflow)stream.readObject();
  }

  /** Sets the non-null Contexts of the subtasks in the workflow to be
   * that of the parent task
   **/
  private void setContext() {
    Context context = task.getContext();
    // No sense in going through all the subtasks if the parent's Context was never set.
    if (context == null)
      return;

    // Set the Context of the subtasks to be the Context of the parent task
    for (Enumeration e = workflow.getTasks(); e.hasMoreElements();) {
      Object o = e.nextElement();
      if (o instanceof TaskImpl) {
	TaskImpl subtask = (TaskImpl)o;
	if (subtask.getContext() == null) {
	  subtask.setContext(context);
	}
      }
    }
  }
  public String toString() {
    return "[Expansion to "+workflow+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("workflow", ExpansionImpl.class, "getWorkflow", null));
  }
}
