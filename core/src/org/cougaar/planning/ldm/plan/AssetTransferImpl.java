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

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.mts.MessageAddress;

import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.ActiveSubscriptionObject;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import java.util.*;
import java.beans.*;

/** AssetTransferImpl.java
 * Implementation for assetTransfer
 */
 
public class AssetTransferImpl extends PlanElementImpl 
  implements AssetTransfer, RoleScheduleConflicts
{
 
  private transient Asset asset;     // changed to transient : Persistence
  private transient Asset assigneeAsset;
  private MessageAddress assignerCluster;
  private Schedule assetSchedule;
  private Role theRole;
  private transient boolean potentialconflict = false;
  private transient boolean assetavailconflict = false;
  private transient boolean checkconflict = false;

  private transient boolean isAssetChanged = false;

  //private String role = "RemoteAssignment";
  
  public AssetTransferImpl() {}

  /** Constructor that assumes there is not a good estimated result at this time.
   * @param p
   * @param t
   * @param a  The asset being transferred
   * @param s  The schedule representing the time frame that the asset is being transferred for.
   * @param to  The Cluster that will receive this asset for use
   * @param from  The Cluster that is provided this asset for use
   */
  public AssetTransferImpl(Plan p, Task t, Asset a, Schedule s, Asset to, MessageAddress from) {
    super(p, t);
    setAsset(a);
    setSchedule(s);
    setAssignee(to);
    setAssignor(from);
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done in publishAdd
  }
	
  
  /** Constructor 
   * @param p
   * @param t
   * @param a  The asset being transferred
   * @param s  The schedule representing the time frame that the asset is being transferred for.
   * @param to  The Cluster that will receive this asset for use
   * @param from  The Cluster that is provided this asset for use
   * @param estimatedresult
   * @param aRole
   */
  public AssetTransferImpl(Plan p, Task t, Asset a, Schedule s, Asset to, MessageAddress from, AllocationResult estimatedresult, Role aRole) {
    super(p, t);
    setAsset(a);
    setSchedule(s);
    setAssignee(to);
    setAssignor(from);
    setEstimatedResult(estimatedresult);
    this.theRole = aRole;
    // add myself to the asset's roleschedule
    //doRoleSchedule(this.getAsset());
    // done during publishAdd
  }
  
  
  //AssetTransfer interface implementations
  
  /**
   * @return Asset - Asset being assigned
   */
  public Asset getAsset() {
    return asset;
  }
  
  /**
   * @return Asset - The Asset to which the Asset is being assigned
   */
  public Asset getAssignee() {
    return assigneeAsset;
  }
  
  /** Returns the Cluster that the asset is assigned from.
   * @return MessageAddress representing the source of the asset
   */
 	
  public MessageAddress getAssignor() {
    return assignerCluster;
  }
 
  /** Returns the Schedule for the "ownership" of the asset being transfered.
   *  @return Schedule
   */
  public Schedule getSchedule() {
    return assetSchedule;
  }
  
  public long getStartTime() {
    return (assetSchedule == null)?MIN_VALUE:assetSchedule.getStartTime();
  }

  public long getEndTime() {
    return (assetSchedule == null)?MAX_VALUE:assetSchedule.getEndTime();
  }


  /** Checks to see if there is a protential conflict with another allocation
   * to the same asset.
   * Will return true if there is a potential conflict.
   * Will return false if there is NOT a potential conflict.
   * @return boolean
   */
  public boolean isPotentialConflict() {
    throw new RuntimeException("isPotentialConflict is temporarily deprecated.");
    //return potentialconflict;
  }
  
  /** Checks to see if there is a potential conflict with the asset's
   * available schedule.  ( Asset.getRoleSchedule().getAvailableSchedule() )
   * Will return true if there is a potential conflict.
   * @return boolean
   */
  public boolean isAssetAvailabilityConflict() {    
    throw new RuntimeException("isAssetAvailabilityConflict is temporarily deprecated.");
    //return assetavailconflict;
  }
  
  /** Return the Role this Asset is performing while transferred.
   *  @return Role
   **/
  public Role getRole() {
    return theRole;
  }
 	
 
  //NewAssetTransfer inteface implementations
 	
  /**
   * @param anAsset - set the Asset being assigned
   */
  private void setAsset(Asset anAsset) {
    asset = anAsset;
  }
  
  /**
   * @param toAsset - Set the Asset being assigned the Asset
   */
  private void setAssignee(Asset toAsset) {
    assigneeAsset = toAsset;
  }
  
  /** Sets the Cluster that the asset is assigned from.
   * @param aCluster
   */
 	
  private void setAssignor(MessageAddress aCluster) {
    assignerCluster = aCluster;
  }
 
  /** Sets the Schedule for the "ownership" of the asset being transfered.
   *  @param aSchedule
   */
  private void setSchedule(Schedule aSchedule) {
    assetSchedule = aSchedule;
  }
  
   
  /** Set the estimated allocation result so that a notification will
   * propagate up another level.
   * @param estimatedresult
   */
  public void setEstimatedResult(AllocationResult estimatedresult) {
    super.setEstimatedResult(estimatedresult);
    setCheckConflicts(true);
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
  
  synchronized public void indicateAssetChange() {
    isAssetChanged = true;
  }
  /** Infrastructure only: this actually should be locked
   * before calling this method so that we can tell if it
   * has changed again.
   **/
  synchronized public void resetAssetChangeIndicated() {
    isAssetChanged = false;
  }
  
  synchronized public boolean isAssetChangeIndicated() {
    return isAssetChanged;
  }

  // ActiveSubscriptionObject
  public void addingToBlackboard(Subscriber s) {
    super.addingToBlackboard(s);

    // check for conflicts.
    addToRoleSchedule(asset);
  }
  public void changingInBlackboard(Subscriber s) {
    super.changingInBlackboard(s);

    // check for conflicts
  }
  public void removingFromBlackboard(Subscriber s) {
    super.removingFromBlackboard(s);

    // check for conflicts
    removeFromRoleSchedule(asset);
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(asset);
    stream.writeObject(assigneeAsset);
  }

 
  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();

    asset = (Asset)stream.readObject();
    assigneeAsset = (Asset)stream.readObject();
  }

  public String toString() {
    return "[Transfer of "+asset+" from "+assignerCluster+" to "+assigneeAsset+"]";
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("asset", AssetTransferImpl.class, "getAsset", null));
    c.add(new PropertyDescriptor("role", AssetTransferImpl.class, "getRole", null));
    c.add(new PropertyDescriptor("assignee", AssetTransferImpl.class, "getAssignee",null));
    c.add(new PropertyDescriptor("assignor", AssetTransferImpl.class, "getAssignor",null));
    c.add(new PropertyDescriptor("schedule", AssetTransferImpl.class, "getSchedule", null));
  }

}
