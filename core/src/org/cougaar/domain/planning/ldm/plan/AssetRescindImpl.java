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

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

import org.cougaar.core.cluster.ClusterIdentifier;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.AssetRescind;
import org.cougaar.domain.planning.ldm.plan.NewAssetRescind;

import org.cougaar.domain.planning.ldm.plan.Plan;

/** AssetRescind implementation
 * AssetRescind allows a asset to be rescinded from the Plan. 
 **/


public class AssetRescindImpl extends DirectiveImpl
  implements
  AssetRescind,
  NewAssetRescind
{

  private transient Asset rescindedAsset;
  private transient Asset rescindeeAsset;
  private Schedule rescindedSchedule;
        
  /**
   * @param src
   * @param dest
   * @param assetUID
   * @return AssetRescindImpl
   **/
  public AssetRescindImpl(ClusterIdentifier src, ClusterIdentifier dest, Plan plan,
                          Asset rescindedAsset, Asset rescindeeAsset, 
                          Schedule rescindSchedule) {
    setSource(src);
    setDestination(dest);
    super.setPlan(plan);
    
    setAsset(rescindedAsset);
    setRescindee(rescindeeAsset);
    setSchedule(rescindSchedule);
  }

  /**
   * Returns the asset to be rescinded
   * @return Asset
   **/

  public Asset getAsset() {
    return rescindedAsset;
  }
    
  /**
   * Sets the asset to be rescinded
   * @param Asset
   **/

  public void setAsset(Asset asset) {
    rescindedAsset = asset;
  }
     


  public Asset getRescindee() {
    return rescindeeAsset;
  }
		
  public void setRescindee(Asset newRescindeeAsset) {
    rescindeeAsset = newRescindeeAsset;
  }


  public Schedule getSchedule() {
    return rescindedSchedule;
  }
		
  public void setSchedule(Schedule sched) {
    rescindedSchedule = sched;
  }
       
  public String toString() {
    String scheduleDescr = "(Null RescindedSchedule)";
    if (rescindedSchedule != null) 
      scheduleDescr = rescindedSchedule.toString();
    String assetDescr = "(Null RescindedAsset)";
    if (rescindedAsset != null)
      assetDescr = rescindedAsset.toString();
    String toAssetDescr = "(Null RescindeeAsset)";
    if (rescindeeAsset != null) 
      toAssetDescr = rescindeeAsset.toString();


    return "<AssetRescind "+assetDescr+", "+ scheduleDescr + 
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
    
    stream.writeObject(rescindedAsset);
    stream.writeObject(rescindeeAsset);
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    rescindedAsset = (Asset)stream.readObject();
    rescindeeAsset = (Asset)stream.readObject();
  }

}
