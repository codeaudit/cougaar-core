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

import org.cougaar.domain.planning.ldm.asset.Asset;

import org.cougaar.domain.planning.ldm.plan.AssetVerification;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewAssetVerification;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;


/** An implementation of AssetVerification
 */
public class AssetVerificationImpl extends DirectiveImpl
  implements AssetVerification, NewAssetVerification
{
  private transient Asset myAsset;
  private transient Asset myAssignee;
  private Schedule mySchedule;
                
  //no-arg constructor
  public AssetVerificationImpl() {
  }

  public AssetVerificationImpl(Asset asset, Asset assignee, Schedule schedule) {
    setAsset(asset);
    setAssignee(assignee);
    setSchedule(schedule);
  }


  /** implementation of the AssetVerification interface */

  /** 
   * Returns the asset the verification is in reference to.
   * @return asset
   **/
  public Asset getAsset() {
    return myAsset;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the asset the notification is in reference to.
   * @param asset Asset
   **/
                
  public void setAsset(Asset asset) {
    myAsset = asset;
  }


  /** implementation of the AssetVerification interface */

  /** 
   * Returns the asset the verification is in reference to.
   * @return asset
   **/
  public Asset getAssignee() {
    return myAssignee;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the asset the notification is in reference to.
   * @param asset Asset
   **/
                
  public void setAssignee(Asset assignee) {
    myAssignee = assignee;
  }

  /** implementation of the AssetVerification interface */

  /** 
   * Returns the schedule to be verified
   * @return Schedule
   **/
  public Schedule getSchedule() {
    return mySchedule;
  }
  
  /** implementation methods for the NewNotification interface **/

  /** 
   * Sets the schedule to be verified
   * @param schedule Schedule
   **/
  public void setSchedule(Schedule schedule) {
    mySchedule = schedule;
  }

  
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(myAsset);
    stream.writeObject(myAssignee);
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();

    myAsset = (Asset)stream.readObject();
    myAssignee = (Asset)stream.readObject();
  }

  public String toString() {
    return "<AssetVerification for asset " + myAsset + 
      " assigned to " + myAssignee + ">";
  }
}



