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

import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Role;

import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.ActiveSubscriptionObject;

import java.util.*;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


/** AllocationImpl.java
 * Implementation for allocation
 */
 
public class AllocationImpl extends PlanElementImpl 
  implements Allocation, RoleScheduleConflicts, AllocationforCollections
 {

  private transient Asset asset;   // changed to transient : Persistence

  private transient Task allocTask = null; // changed to transient : Persistence
  private transient boolean potentialconflict = false;
  private transient boolean stale = false;
  private transient boolean assetavailconflict = false;
  private transient boolean checkconflict = false;
  private Role theRole;

   public AllocationImpl() {}

  /* Constructor that takes the Asset, and assumes that there is not a good
   * estimate of the result for now.
   * @param p
   * @param t
   * @param a
   */
  public AllocationImpl(Plan p, Task t, Asset a) {
    super(p, t);
    setAsset(a);
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd now.
  }
  
  /* Constructor that takes the Asset, and an initial estimated result
   * @param p
   * @param t
   * @param a
   * @param estimatedresult
   */
  public AllocationImpl(Plan p, Task t, Asset a, AllocationResult estimatedresult, Role aRole) {
    super(p, t);
    setAsset(a);
    estAR = estimatedresult;
    this.theRole = aRole;
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd
  }
  
   
  /** Set the estimated allocation result so that a notification will
    * propagate up another level.
    * @param estimatedresult
    */
  public void setEstimatedResult(AllocationResult estimatedresult) {
    super.setEstimatedResult(estimatedresult);
    setCheckConflicts(true);
  }
 	
  /**
   * @return Asset - Asset associated with this allocation/subtask
   */
  public Asset getAsset() {
    return asset;
  }
  
  /**
    * @return boolean - true if there is a potential conflict with another 
    * allocation to the same asset.
    * @deprecated
    */
  public boolean isPotentialConflict() {
    //throw new RuntimeException("isPotentialConflict is temporarily deprecated.");
    System.err.println("AllocationImpl::isPotentialConflict() - a temporarily deprecated method - has been called");    
    return potentialconflict;
  }
  
  /** Checks to see if there is a potential conflict with the asset's
    * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
    * Will return true if there is a potential conflict.
    * @return boolean
    * @deprecated
    */
  public boolean isAssetAvailabilityConflict() {
//   throw new RuntimeException("isAssetAvailabilityConflict is temporarily deprecated.");
    System.err.println("AllocationImpl::isAssetAvailabilityConflict() - a temporarily deprecated method - has been called");    
   return assetavailconflict;
  }
  
  /** Check to see if this allocation is Stale and needs to be revisited.
    * Will return true if it is stale (needs to be revisted)
    * @return boolean
    */
  public boolean isStale() {
    return stale;
  }
  
  /** Return the Role that the Asset is performing while executing this PlanElement (Task).
   * @return Role
   **/
  public Role getRole() {
    return theRole;
  }
  
  /** Set the stale flag.  Usualy used by Trigger actions.
    * @param stalestate
    */
  public void setStale(boolean stalestate) {
    stale = stalestate;
  }

  /**
   * @param anAsset - set Asset associated with this allocation/subtask
   */
  private void setAsset(Asset anAsset) {
    asset = anAsset;
  }
  
 	
  /* INFRASTRUCTURE ONLY */
  public void setPotentialConflict(boolean conflict) {
    potentialconflict = conflict;
  }
  /* INFRASTRUCTURE ONLY */
  public void setAssetAvailabilityConflict(boolean availconflict) {
    assetavailconflict = availconflict;
  }
  /* INFRASTRUCTURE ONLY */
  public void setCheckConflicts(boolean check) {
    checkconflict = check;
  }
  /* INFRASTRUCTURE ONLY */
  public boolean checkConflicts() {
    return checkconflict;
  }
  

  // ActiveSubscriptionObject
  public boolean addingToLogPlan(Subscriber s) {
    if (!super.addingToLogPlan(s)) return false;
    addToRoleSchedule(asset);
    // check for conflicts.
    return true;
  }
  public boolean changingInLogPlan(Subscriber s) {
    if (!super.changingInLogPlan(s)) return false;
    // check for conflicts
    return true;
  }
  public boolean removingFromLogPlan(Subscriber s) {
    if (!super.removingFromLogPlan(s)) return false;
    removeFromRoleSchedule(asset);
    // check for conflicts
    return true;
  }

  public Task getAllocationTask() { return allocTask; }
  public void setAllocationTask(Task t) { allocTask = t; }

	
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(asset);
    stream.writeObject(allocTask);
 }



  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    /** ----------
      *    READ handlers common to Persistence and
      *    Network serialization.  NOte that these
      *    cannot be references to Persistable objects.
      *    defaultReadObject() is likely to belong here...
      * ---------- **/
    stream.defaultReadObject();
    asset = (Asset)stream.readObject();
    allocTask = (Task)stream.readObject();
  }

  public String toString() {
    return "[Allocation of " + getTask().getUID() + " to "+asset+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("asset", AllocationImpl.class, "getAsset", null));
    c.add(new PropertyDescriptor("role", AllocationImpl.class, "getRole", null));
    c.add(new PropertyDescriptor("allocationTask", AllocationImpl.class, "getAllocationTask", null));
    c.add(new PropertyDescriptor("stale", AllocationImpl.class, "isStale", null));
  }

}
