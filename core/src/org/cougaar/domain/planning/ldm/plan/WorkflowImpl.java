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

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.ActiveSubscriptionObject;
import org.cougaar.core.cluster.ClaimableImpl;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.XMLizable;
import org.cougaar.util.XMLize;
import org.cougaar.util.Empty;

import java.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.cougaar.core.society.UID;

import org.cougaar.core.plugin.Annotation;

/**
 * This class implements Workflow 
 **/

public class WorkflowImpl 
  extends ClaimableImpl
  implements Workflow, NewWorkflow, XMLizable, java.io.Serializable
{
  private transient Task basetask;
  // protected access for MPWorkflowImpl
  protected transient Vector subtasks = new Vector();
  private transient Vector constraints = new Vector();
  private UID uid;
  private ClusterIdentifier owner;
  private transient AllocationResultAggregator currentARA = AllocationResultAggregator.DEFAULT;
  private transient AllocationResult cachedar = null;

  public String toString() {
    return "<workflow " +  uid + " of base task "  + basetask + " of  "  + subtasks.size() + " tasks " + "and " + constraints.size() + " constraints>";
  }

  public WorkflowImpl(ClusterIdentifier owner, UID uid) {
    this.owner = owner;
    this.uid = uid;
  }

  /**@return Task for which this Workflow is an expansion */
  public Task getParentTask() { return basetask; }

        
  /** @return Enumeration{Task} Enumerate over our subtasks. */
  public Enumeration getTasks() { 
    return subtasks.elements(); 
  }


  /** @return Enumeration{Constraint} Enumerate over the constraints. */
  public Enumeration getConstraints() { 
    return constraints.elements(); 
  }
  
  /** 
   *@param task - task which you are inquiring about
   *@return Enumeration{Constraint} Enumerate over constraints that related to the passed in task. */
  public Enumeration getTaskConstraints(Task task) {
    Enumeration c = constraints.elements();
    Vector contasks = new Vector();
    while (c.hasMoreElements() ) {
      Constraint ct = (Constraint) c.nextElement();
      if ((task.equals(ct.getConstrainedTask())) || (task.equals(ct.getConstrainingTask())) ) {
        contasks.addElement(ct);
      }
    }
    return contasks.elements();
  }
                
  /** @param constrainedTask - the task being constrained
   * @param constrainingTask - the task that is constraining another task
   *@return Enumeration{Constraints} - Constraints that are related to both tasks */       
  public Enumeration getPairConstraints(Task constrainedTask, Task constrainingTask) {
    
    Enumeration c = constraints.elements();
    Vector contasks = new Vector();
    while (c.hasMoreElements() ) {
      Constraint ct = (Constraint) c.nextElement();
      if ((constrainedTask.equals(ct.getConstrainedTask())) && (constrainingTask.equals(ct.getConstrainingTask())) ) {
        contasks.addElement(ct);
      }
    }
    return contasks.elements();
  }

 
  private transient TaskScoreTable _tst = null;
  private transient Task[] _tasks;
  private transient AllocationResult[] _ars;

  private synchronized void clearTST() {
    _tst = null;
    _tasks = null;
    _ars = null;
  }

  private TaskScoreTable updateTST() {
    synchronized (subtasks) {

      if (_tst == null) {
  	_tasks = new Task[subtasks.size()];
  	_tasks = (Task[]) subtasks.toArray(_tasks);
	if (_tasks.length == 0) return null;
	_ars = new AllocationResult[_tasks.length];
        _tst = new TaskScoreTable(_tasks, _ars);
      }
    
      for (int i=0; i<_ars.length; i++) {
        PlanElement pe = _tasks[i].getPlanElement();
        if (pe != null) {
          _ars[i] = pe.getEstimatedResult();
        }
      }
      return _tst;
    }
  }

  
  /** Calls calculate on the defined AllocationResultAggregator
   * @return a new AllocationResult representing aggregation of 
   * all subtask results
   */
  public synchronized AllocationResult aggregateAllocationResults() {
    TaskScoreTable tst = updateTST();
    if (tst == null) return null;
    // call calculate on the PenaltyValueAggregator
    AllocationResult newresult = currentARA.calculate(this, tst, cachedar);
    cachedar = newresult;
    return newresult;
  }

  /** Conses a new TaskScoreTable each time **/
  public synchronized TaskScoreTable getCurrentTST() {
    if (_tasks == null) return null;
    int l = _tasks.length;
    if (l == 0) return null;
    Task[] ts = new Task[l];
    AllocationResult[] ars = new AllocationResult[l];
    for (int i=0; i<l; i++) {
      ts[i] = _tasks[i];
      ars[i] = _ars[i];
    }
    return new TaskScoreTable(ts, ars);
  }

  /** Has a constraint been violated?
   **/
  public boolean constraintViolation() {
    for (Enumeration cons = constraints.elements(); cons.hasMoreElements(); ) {
      Constraint c = (Constraint) cons.nextElement();
      if (isConstraintViolated(c)) {
        return true;
      }
    }
    return false;
  }
  
  /** Get the constraints that were violated.
    * @return Enumeration{Constraint}
    */
    
  public Enumeration getViolatedConstraints() {
    Vector violations = getViolatedConstraintsVector(false);
    if (violations == null || violations.size() == 0) {
      return Empty.enumeration;
    }
    return violations.elements();
  }

  private Vector getViolatedConstraintsVector(boolean firstOnly) {
    // check to see if there are any constraints
    if (constraints.size() == 0) return null;
    Vector violations = new Vector(constraints.size());
    for (Enumeration cons = constraints.elements(); cons.hasMoreElements(); ) {
      Constraint c = (Constraint) cons.nextElement();
      if (isConstraintViolated(c)) {
        violations.addElement(c);
        if (firstOnly) break;
      }
    }
    return violations;
  }
    
  //setter method implementations from NewWorkflow

  /** @param parentTask set the parent task */
  public void setParentTask(Task parentTask) {
    basetask = parentTask;
  }  
  
  /** @param tasks set the tasks of the Workflow */     
  public void setTasks(Enumeration tasks) {
    if (tasks == null) {
        throw new IllegalArgumentException("Workflow.setTasks(Enum e): e must be a non-null Enumeration");
    }
    synchronized (subtasks) {
      subtasks.removeAllElements();
      while (tasks.hasMoreElements()) {
        Task t = (Task) tasks.nextElement();
        if ( t != null ) {
          subtasks.addElement(t);
        } else {
          // buzzz... wrong answer - tried to pass in a null!
          throw new IllegalArgumentException("Workflow.setTasks(Enum e): all elements of e must be Tasks");
        }
      }
      clearTST();
    }
  } 
   
  /** @param newTask addTask allows you to add a Task to a Workflow.*/
  public void addTask(Task newTask) {
    if (newTask == null) {
      // buzzzz wrong answer - tried to pass in a null!!
      throw new IllegalArgumentException("Workflow.addTask(arg): arg must be a non-null Task");
    }
    subtasks.addElement(newTask);
    clearTST();
  }
  
  /** @param remTask Remove the specified Task from the Workflow's sub-task collection  **/
  public void removeTask(Task remTask) {
    subtasks.removeElement(remTask);
    clearTST();
  }
  
  /** Note any previous values will be dropped.
   * @param enumofConstraints setConstraints allows you to set the Enumeration
   * of Constraints of a Workflow.  */
  public void setConstraints(Enumeration enumofConstraints) {
    if (enumofConstraints == null) {
      throw new IllegalArgumentException("Workflow.setConstraints(Enum e): illegal null argument");
    }
    constraints.removeAllElements();
    while (enumofConstraints.hasMoreElements()) {
      Object o = enumofConstraints.nextElement();
      if (o instanceof Constraint) {
        Constraint c = (Constraint) o;
        if (checkConstraintAspects(c)) {
          constraints.addElement(c);
        } else {
          throw new IllegalArgumentException("Workflow.setConstraints(): incompatible aspects");
        }
      } else {
        //buzzzz... wrong answer - tried to pass in a null!
        throw new IllegalArgumentException("Workflow.setConstraints(Enum e): all elements of e must be Constraints");
      }
    }
  }

  private static boolean checkConstraintAspects(Constraint constraint) {
    int constrainingAspectType = constraint.getConstrainingAspect();
    int constrainedAspectType = constraint.getConstrainedAspect();
    return !(constrainingAspectType != constrainedAspectType
             && constrainingAspectType != AspectType.END_TIME
             && constrainingAspectType != AspectType.START_TIME
             && constrainedAspectType != AspectType.END_TIME
             && constrainedAspectType != AspectType.START_TIME);
  }

  /** @param newConstraint addConstraint allows you to add a Constraint to a Workflow. */
  public void addConstraint(Constraint newConstraint) {
    if (newConstraint != null) {
      if (!checkConstraintAspects(newConstraint)) {
        throw new IllegalArgumentException("Workflow.addConstraint(): incompatible aspects");
      }
      constraints.addElement(newConstraint);
    } else {
      //buzzz... wrong answer - tried to pass in a null!
      throw new IllegalArgumentException("Workflow.addConstraint(): illegal null argument");
    }
  }

  /**
   * Returns first constraint for which the constraining event is
   * defined and constrained event is undefined or violated with
   * respect to constraining event.
   **/

  public Constraint getNextPendingConstraint()
  {
    Vector violations = getViolatedConstraintsVector(true);
    for (int i = 0; i < constraints.size(); i++) {
      Constraint c = (Constraint) constraints.elementAt(i);
      if (isConstraintViolated(c)) return c;
    }
    return null;
  }

  public boolean isConstraintViolated(Constraint c) {
    ConstraintEvent ce1 = c.getConstrainingEventObject();
    ConstraintEvent ce2 = c.getConstrainedEventObject();

    double constrainingValue = ce1.getValue();
    if (constrainingValue == ConstraintEvent.NOVALUE) return false;

    double constrainedValue = ce2.getValue();
    if (constrainedValue == ConstraintEvent.NOVALUE) return true;

    double diff = constrainedValue - constrainingValue + c.getOffsetOfConstraint();
    switch (c.getConstraintOrder()) {
    case Constraint.BEFORE: // Same as LESSTHAN
      if (diff <= 0.0) return true;
      break;
    case Constraint.AFTER: // Same as GREATERTHAN
      if (diff >= 0.0) return true;
      break;
    case (Constraint.COINCIDENT): // Same as EQUALTO
      if (diff == 0.0) return true;
      break;
    }
    return false;               // Bogus constraint
  }
  
  /** sets a specific compute algorithm to use while computing the aggregated
    * allocation results of the workflow.  If this method is not called, the allocationresult
    * will be aggregated using the default AllocationResultAggregator (Sum).
    * @param aragg The AllocationResultAggregator to use.
    * @see org.cougaar.domain.planning.ldm.plan.AllocationResultAggregator
    */
  public void setAllocationResultAggregator(AllocationResultAggregator aragg) {
    currentARA = aragg;
  }
  
  /** Return the Unique ID number for this object */
  public UID getUID() {
    return uid;
  }

  public void setUID(UID u) {
    if (uid != null) throw new IllegalArgumentException("UID already set");
    uid = u;
  }

  public ClusterIdentifier getOwner() { return owner; }


  /** serialize workflows by proxying the tasks all the tasks referred to
   **/
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(basetask);
    stream.writeObject(subtasks);
    stream.writeObject(constraints);
    if (currentARA == AllocationResultAggregator.DEFAULT) {
      stream.writeObject(null);
    } else {
      stream.writeObject(currentARA);
    }
    if (stream instanceof org.cougaar.core.cluster.persist.PersistenceOutputStream) {
        stream.writeObject(myAnnotation);
    }
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();

    basetask = (Task) stream.readObject();
    subtasks = (Vector) stream.readObject();
    constraints = (Vector) stream.readObject();
    currentARA = (AllocationResultAggregator) stream.readObject();
    if (currentARA == null) {
      currentARA = AllocationResultAggregator.DEFAULT;
    }
    if (stream instanceof org.cougaar.core.cluster.persist.PersistenceInputStream) {
      myAnnotation = (Annotation) stream.readObject();
    }
  }

  // new property reading methods returned by WorkflowImplBeanInfo

  public String getParentTaskID() {
    return getParentTask().getUID().toString();
  }

  public String[] getTaskIDs() {
    synchronized (subtasks) {
      String taskID[] = new String[subtasks.size()];
      for (int i = 0; i < subtasks.size(); i++)
        taskID[i] = ((Task)subtasks.elementAt(i)).getUID().toString();
      return taskID;
    }
  }

  public String getTaskID(int i) {
    String taskID[] = getTaskIDs();
    if (i < taskID.length)
      return taskID[i];
    else
      return null;
  }

  public Constraint[] getConstraintsAsArray() {
    return (Constraint []) constraints.toArray(new Constraint[constraints.size()]);
  }

  public AllocationResult getAllocationResult() {
    return cachedar;
  }

  // WARNING: STUBBED FOR NOW
  public Constraint[] getViolatedConstraintsAsArray() {
    Vector violations = getViolatedConstraintsVector(false);
    if (violations == null) return new Constraint[0];
    return (Constraint[]) violations.toArray(new Constraint[violations.size()]);
  }
 
  private boolean _propagateP = true;
  public boolean isPropagatingToSubtasks() { return _propagateP; }
  public void setIsPropagatingToSubtasks(boolean isProp) { _propagateP = isProp; }
  /** @deprecated  Use setIsPropagatingToSubtasks(boolean isProp) -defaults to true*/
  public void setIsPropagatingToSubtasks() { _propagateP = true; }
  
  // used by ExpansionImpl for infrastructure propagating rescinds.
  public List clearSubTasks() {
    ArrayList l = null;
    l = new ArrayList(subtasks);
    subtasks.removeAllElements();
    return l;
  }

  // 
  // XMLizable method for UI, other clients
  //
  public Element getXML(Document doc) {
    return XMLize.getPlanObjectXML(this,doc);
  }

  private transient Annotation myAnnotation = null;
  public void setAnnotation(Annotation pluginAnnotation) {
    myAnnotation = pluginAnnotation;
  }
  public Annotation getAnnotation() {
    return myAnnotation;
  }


}
