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
import org.cougaar.planning.ldm.plan.AssetAssignment;
import org.cougaar.planning.ldm.plan.Directive;
import org.cougaar.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.core.agent.ClusterIdentifier;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
 

/** AssetAssignmentImpl implementation
 * skeleton implementation of assetassignment
 */

public class AssetAssignmentImpl extends DirectiveImpl
  implements AssetAssignment, NewAssetAssignment
{
		
  private transient Asset assignedAsset;  // changed to transient : Persistence
  private transient Asset assigneeAsset;
  private Schedule assignSchedule;
  private boolean _isUpdate = false;
  private byte _kind = NEW;

  //no-arg constructor
  public AssetAssignmentImpl () {
    super();
  }
		
  //constructor that takes one Asset
  public AssetAssignmentImpl (Asset a) {
    assignedAsset = a;
  }
		
  //constructor that takes multiple Assets
  /*
  public AssetAssignmentImpl (Enumeration as) {
    assignedAssets = new Vector();
    while (as.hasMoreElements()) {
      Asset asset = (Asset) as.nextElement();
      assignedAssets.addElement(asset);
    }
  }
  */
		
  //constructor that takes the Asset, the plan, the schedule
  // the source cluster and the destination asset
  public AssetAssignmentImpl (Asset as, Plan p, Schedule s, 
                              ClusterIdentifier sc, Asset da) {
    assignedAsset = as;
    super.setPlan(p);
    assignSchedule = s;
    super.setSource(sc);

    assigneeAsset = da;
    if (!assigneeAsset.hasClusterPG()) {
      throw new IllegalArgumentException("AssetAssignmentImpl: destination asset - " + assigneeAsset + " - does not have a ClusterPG");
    }
    super.setDestination(assigneeAsset.getClusterPG().getClusterIdentifier());
  }

		
  /** implementations of the AssetAssignment interface */
		
  public boolean isUpdate() { return _kind == UPDATE; }

  public boolean isRepeat() { return _kind == REPEAT; }

  public void setKind(byte value) { _kind = value; }

  /** @return Asset Asset that is being assigned */
  public Asset getAsset() {
    return assignedAsset;
  }
		
  public void setAsset(Asset newAssignedAsset) {
    assignedAsset = newAssignedAsset;
  }
			
  public Schedule getSchedule() {
    return assignSchedule;
  }
		
  public void setSchedule(Schedule sched) {
    assignSchedule = sched;
  }

  public Asset getAssignee() {
    return assigneeAsset;
  }
		
  public void setAssignee(Asset newAssigneeAsset) {
    assigneeAsset = newAssigneeAsset;
  }
  
  public String toString() {
    String scheduleDescr = "(Null AssignedSchedule)";
    if (assignSchedule != null) 
      scheduleDescr = assignSchedule.toString();
    String assetDescr = "(Null AssignedAsset)";
    if (assignedAsset != null)
      assetDescr = assignedAsset.toString();
    String toAssetDescr = "(Null AssigneeAsset)";
    if (assigneeAsset != null) 
      toAssetDescr = assigneeAsset.toString();
    String kind;
    switch (_kind) {
    case UPDATE: kind = "UPDATE"; break;
    case NEW: kind = "NEW"; break;
    case REPEAT: kind = "REPEAT"; break;
    default: kind = "BOGUS";
    }

    return "<AssetAssignment of " + assetDescr+", " + scheduleDescr + 
      " " + kind + " " + toAssetDescr + ">" + super.toString();
  }


  private void writeObject(ObjectOutputStream stream) throws IOException {

    /** ----------
     *    WRITE handlers common to Persistence and
     *    Network serialization.  NOte that these
     *    cannot be references to Persistable objects.
     *    defaultWriteObject() is likely to belong here...
     * ---------- **/
    stream.defaultWriteObject();
    
    stream.writeObject(assignedAsset);
    stream.writeObject(assigneeAsset);
  }



  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    assignedAsset = (Asset)stream.readObject();
    assigneeAsset = (Asset)stream.readObject();
  }
}
