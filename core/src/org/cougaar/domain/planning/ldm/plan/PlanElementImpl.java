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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.core.util.XMLizable;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;

import org.cougaar.core.cluster.Transaction;
import org.cougaar.core.cluster.*;

import org.cougaar.core.util.XMLize;

import java.util.*;
import org.cougaar.util.SelfDescribingBeanInfo;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.cougaar.core.plugin.Annotation;

import org.cougaar.core.society.UID;

/** PlanElement Implementation
 * PlanElements represent the association of a Plan, a Task,
 * and a Disposition (where a Disposition is either
 * an Allocation, an Expansion, an Aggregation, or an AssetTransfer).
 * A Disposition (as defined above) are subclasses of PlanElement.
 * PlanElements make a Plan.  For Example, a task "move 15 tanks..." with
 * an Allocation(an Asset, estimated penalty and estimated schedule) 
 * of 15 HETs could represent a PlanElement.
 **/

public abstract class PlanElementImpl 
  extends PublishableAdapter
  implements PlanElement, NewPlanElement, PEforCollections, XMLizable, ActiveSubscriptionObject, BeanInfo
{
        
  protected transient Task task;   // changed to transient : Persistence
  //protected Plan plan;
  
  private UID uid;

  /**
   * There are four allocation results:
   * estAR is set by the plugin often as a copy of the reported
   * rcvAR is computed from downstream results (e.g. by workflow aggregators)
   * obsAR is set from observed events (event monitor)
   * repAR is the merge of obsAR and rcvAR and is lazily evaluated
   **/
  protected AllocationResult obsAR = null;
  protected AllocationResult repAR = null;
  protected AllocationResult rcvAR = null;
  protected AllocationResult estAR = null;

  protected transient boolean notify = false;

  //no-arg constructor
  public PlanElementImpl() {}

  public PlanElementImpl(UID uid) {
    this.uid = uid;
  }     
        
  //constructor that takes both a plan and a task object
  public PlanElementImpl (Plan p, Task t) {
    //plan = p;
    setTask(t);
  }
  
  public void setUID(UID uid) { this.uid = uid;}
  public UID getUID() { return uid; }
        
  //PlanElement interface implementations

  /**
   * @return Plan  the plan this planelement is a part of
   **/
  public Plan getPlan() {
    return PlanImpl.REALITY;
  }
               
  /** This returns the Task of the PlanElement.
   * @return Task
   **/
        
  public Task getTask() {
    return task;
  }

  // NewPlanElement interface implementations
 
  /** This sets the Task of the PlanElement. 
   * Also sets the planelement  of the task
   * @param t
   **/
        
  public void setTask(Task t) {
    task = t;
  }
  
  /** @param p - set the plan this planelement is a part of */
  public void setPlan(Plan p) {
    //plan = p;
  }
  
  /** Returns the estimated allocation result that is related to performing
   * the Task.
   * @return AllocationResult
   **/
  
  public AllocationResult getEstimatedResult() {
    return estAR;
  }
  
  /** Returns the reported allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getReportedResult() {
    if (repAR == null) {
      if (rcvAR == null) {
        repAR = obsAR;
      } else if (obsAR == null) {
        repAR = rcvAR;
      } else {
        repAR = new AllocationResult(obsAR, rcvAR);
      }
    }
    return repAR;
  }
  
  /** Returns the received allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getReceivedResult() {
    return rcvAR;
  }
  
  /** Returns the observed allocation result.
   * @return AllocationResult
   **/
  public AllocationResult getObservedResult() {
    return obsAR;
  }

  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   **/
  public void setEstimatedResult(AllocationResult estimatedresult) {
    estAR = estimatedresult;
    Transaction.noteChangeReport(this,new PlanElement.EstimatedResultChangeReport());
    setNotification(true);
  }
  
  /**
   * CALLED BY INFRASTRUCTURE ONLY - AFTER RESULTS HAVE BEEN COMPUTED ACROSS TASKS.
   * @param rcvres the new received AllocationResult object associated with this pe 
   */
  public void setReceivedResult(AllocationResult rcvres) {
    rcvAR = rcvres;
    repAR = null;               // Need to recompute this
    Transaction.noteChangeReport(this,new PlanElement.ReportedResultChangeReport());
  }

  /** @deprecated use setReceivedResult **/
  public void setReportedResult(AllocationResult repres) {
    throw new UnsupportedOperationException("Use setReceivedResult instead");
  }
  
  /**
   * Set or update the observed AllocationResult. Should be called
   * only by the event monitor.
   * @param obsres the new observed AllocationResult object associated with this pe 
   **/
  public void setObservedResult(AllocationResult obsres) {
    obsAR = obsres;
    repAR = null;               // Need to recompute this
    Transaction.noteChangeReport(this, new PlanElement.ObservedResultChangeReport());
    Transaction.noteChangeReport(this, new PlanElement.ReportedResultChangeReport());
  }
  
  // implement TimeSpan

  public long getStartTime() {
    AllocationResult ar = estAR;
    if (ar != null) {
      if (ar.isDefined(AspectType.START_TIME)) {
        return (long) ar.getValue(AspectType.START_TIME);
      }
    }
    return MIN_VALUE;
  }

  public long getEndTime() {
    AllocationResult ar = estAR;
    if (ar != null) {
      if (ar.isDefined(AspectType.END_TIME)) {
        return (long) ar.getValue(AspectType.END_TIME);
      }
    }
    return MAX_VALUE;
  }

  public boolean shouldDoNotification() {
    return notify;
  }
  public void setNotification(boolean v) {
    notify = v;
  }
  
  // If the planelement is either an allocation or an assettransfer, add the 
  // planelement to the respective Asset's RoleSchedule.
  protected void addToRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.addToRoleSchedule(this);
    } else {
      System.err.println("\n WARNING - could not add PlanElement to roleschedule");
    }
  }
  protected void removeFromRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.removeFromRoleSchedule(this);
    } else {
      System.err.println("\n WARNING - could not remove PlanElement from roleschedule");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
 
    stream.defaultWriteObject();
 
    stream.writeObject(task);
    if (stream instanceof org.cougaar.core.cluster.persist.PersistenceOutputStream) {
        stream.writeObject(myAnnotation);
    }
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    task = (Task)stream.readObject();
    if (stream instanceof org.cougaar.core.cluster.persist.PersistenceInputStream) {
        myAnnotation = (Annotation) stream.readObject();
    }
  }

  public String toString() {
    return "[PE #"+task.getUID()+" -> "+"]";
  }

  // ActiveSubscriptionObject
  public boolean addingToLogPlan(Subscriber s) {
    Task t = getTask();
    Date comdate = t.getCommitmentDate();
    if (comdate != null) {
      // make sure the current alp time is before commitment time
      if ( s.getClient().currentTimeMillis()  > comdate.getTime() ) {
        // its after the commitment time - don't publish the object
        return false;
      }
    }

    if (t.getPlanElement() == null) {
      ((TaskImpl)t).privately_setPlanElement(this);
    } else {
      return false;
    }

    return true;
  }
  public boolean changingInLogPlan(Subscriber s) {
    return true;
  }
  public boolean removingFromLogPlan(Subscriber s) {
    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();;

    return true;
  }

  private transient Annotation myAnnotation = null;
  public void setAnnotation(Annotation pluginAnnotation) {
    myAnnotation = pluginAnnotation;
  }
  public Annotation getAnnotation() {
    return myAnnotation;
  }

  // 
  // XMLizable method for UI, other clients
  //
  public Element getXML(Document doc) {
    return XMLize.getPlanObjectXML(this,doc);
  }

  //dummy PropertyChangeSupport for the Jess Interpreter.
  public PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
      pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
      pcs.removePropertyChangeListener(pcl);
  }

  // beaninfo - duplicate of SelfDescribingBeanInfo because
  // java doesn't allow multiple inheritence of implementation.

  public BeanDescriptor getBeanDescriptor() { return null; }
  public int getDefaultPropertyIndex() { return -1; }
  public EventSetDescriptor[] getEventSetDescriptors() { return null; }
  public int getDefaultEventIndex() { return -1; }
  public MethodDescriptor[] getMethodDescriptors() { return null; }
  public BeanInfo[] getAdditionalBeanInfo() { return null; }
  public java.awt.Image getIcon(int iconKind) { return null; }
  private static final PropertyDescriptor[] _emptyPD = new PropertyDescriptor[0];
  public PropertyDescriptor[] getPropertyDescriptors() { 
    Collection pds = new ArrayList();
    try {
      addPropertyDescriptors(pds);
    } catch (IntrospectionException ie) {
      System.err.println("Warning: Caught exception while introspecting on "+this.getClass());
      ie.printStackTrace();
    }
    return (PropertyDescriptor[]) pds.toArray(_emptyPD);
  }
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    c.add(new PropertyDescriptor("uid", PlanElementImpl.class, "getUID", null));
    //c.add(new PropertyDescriptor("plan", PlanElementImpl.class, "getPlan", null));
    c.add(new PropertyDescriptor("task", PlanElementImpl.class, "getTask", null));
    c.add(new PropertyDescriptor("estimatedResult", PlanElementImpl.class, "getEstimatedResult", "setEstimatedResult"));
    c.add(new PropertyDescriptor("reportedResult", PlanElementImpl.class, "getReportedResult", null));
  }
}
