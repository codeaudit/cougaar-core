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



