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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.AssetAssignment;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.domain.planning.ldm.plan.NewAssetAssignment;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.core.cluster.ClusterIdentifier;

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


    return "<AssetAssignment "+assetDescr+", "+ scheduleDescr + 
      " to " + toAssetDescr + ">" + super.toString();
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
