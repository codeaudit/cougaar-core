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

import org.cougaar.core.blackboard.ActiveSubscriptionObject;

import org.cougaar.core.blackboard.Subscriber;

import org.cougaar.core.blackboard.PublishableAdapter;
import org.cougaar.core.blackboard.BlackboardException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.util.XMLizable;
import org.cougaar.planning.ldm.plan.AllocationResult;

import org.cougaar.core.blackboard.Transaction;
import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;

import org.cougaar.core.util.XMLize;

import org.cougaar.util.log.*;

import java.util.*;
import org.cougaar.util.SelfDescribingBeanInfo;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.cougaar.core.plugin.Annotation;

import org.cougaar.core.util.UID;

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
  implements PlanElement, NewPlanElement, PEforCollections, ScheduleElement, XMLizable, ActiveSubscriptionObject, BeanInfo
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

  private static final Logger logger = Logging.getLogger(PlanElement.class);

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
  
  // ScheduleElement implementation
  /** Start date is a millisecond-precision, inclusive time of start.
   * @return Date Start time for the task 
   **/
  public Date getStartDate() { return new Date(getStartTime()); }
	
  /** End Date is millisecond-precision, <em>exclusive</em> time of end.
   * @return Date End time for the task 
   **/
  public Date getEndDate() { return new Date(getEndTime()); }
	
  /** is the Date on or after the start time and strictly before the end time?
   *  @return boolean whether the date is included in this time interval.  
   **/
  public boolean included(Date date) {
    return included(date.getTime());
  }
	
  /** is the time on or after the start time and strictly before the end time?
   * @return boolean whether the time is included in this time interval 
   **/
  public boolean included(long time) {
    return ( (time >= getStartTime()) && (time < getEndTime()) );
  }

  /** Does the scheduleelement overlap (not merely abut) the schedule?
   * @return boolean whether schedules overlap 
   **/
  public boolean overlapSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime < getEndTime() &&
             tetime > getStartTime() );
  }


  /** Does the scheduleElement meet/abut the schedule?
   **/
  public boolean abutSchedule(ScheduleElement se) {
    long tstime = se.getStartTime();
    long tetime = se.getEndTime();
                
    return ( tstime == getEndTime() ||
             tetime == getStartTime() );
  }


  // If the planelement is either an allocation or an assettransfer, add the 
  // planelement to the respective Asset's RoleSchedule.
  protected void addToRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.add(this);
    } else {
      System.err.println("\n WARNING - could not add PlanElement to roleschedule");
    }
  }
  protected void removeFromRoleSchedule(Asset asset) {
    Asset roleasset = asset;
    if (roleasset != null) {
      RoleScheduleImpl rsi = (RoleScheduleImpl) roleasset.getRoleSchedule();
      rsi.remove(this);
    } else {
      System.err.println("\n WARNING - could not remove PlanElement from roleschedule");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
 
    stream.defaultWriteObject();
 
    stream.writeObject(task);
    if (stream instanceof org.cougaar.core.persist.PersistenceOutputStream) {
        stream.writeObject(myAnnotation);
    }
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    task = (Task)stream.readObject();
    if (stream instanceof org.cougaar.core.persist.PersistenceInputStream) {
        myAnnotation = (Annotation) stream.readObject();
    }
    pcs = new PropertyChangeSupport(this);
  }

  public String toString() {
    return "[PE #"+task.getUID()+" -> "+"]";
  }

  // ActiveSubscriptionObject
  public void addingToBlackboard(Subscriber s) {
    Task t = getTask();
    Date comdate = t.getCommitmentDate();
    if (comdate != null) {
      // make sure the current alp time is before commitment time
      if ( s.getClient().currentTimeMillis()  > comdate.getTime() ) {
        // its after the commitment time - don't publish the object
        logger.warn("publishAdd of "+this+" past commitmenttime "+comdate.getTime());
      }
    }

    if (t.getPlanElement() == null) {
      ((TaskImpl)t).privately_setPlanElement(this);
    } else {
      throw new BlackboardException("publishAdd of miswired PlanElement (task already has PE): "+this);
    }
  }

  public void changingInBlackboard(Subscriber s) {}

  public void removingFromBlackboard(Subscriber s) {
    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();;
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
  public transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

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
