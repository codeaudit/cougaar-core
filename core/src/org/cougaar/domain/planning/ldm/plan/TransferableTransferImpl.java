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
import org.cougaar.domain.planning.ldm.plan.Transferable;
import org.cougaar.domain.planning.ldm.plan.TransferableTransfer;
import org.cougaar.domain.planning.ldm.plan.NewTransferableTransfer;

/** TransferableTransferImpl
  * A Transferable Transfer should be used to transfer a Transferable object to
  * another cluster (org asset).
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: TransferableTransferImpl.java,v 1.1 2000-12-15 20:16:45 mthome Exp $
  */
public class TransferableTransferImpl
  implements TransferableTransfer, NewTransferableTransfer, java.io.Serializable
{
  
  private Transferable thetransferable;
  private Asset thecluster;
  
  /** no-arg constructor - use the setters in the NewTransferableTransfer Interface
    * to build a complete object
    */
  public TransferableTransferImpl() {
    super();
  }
  
  /** Simple constructor 
    * @param aTransferable - the Transferable being sent
    * @param anAsset - An Organization Asset representing the Cluster that the Transferable is being sent to
    */
  public TransferableTransferImpl(Transferable aTransferable, Asset anAsset) {
    super();
    this.setTransferable(aTransferable);
    this.setAsset(anAsset);
  }
  
  /** The Transferable being sent
    * @return Transferable
    */
  public Transferable getTransferable() {
    return thetransferable;
  }
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Cluster.
    * @return Asset
    */
  public Asset getAsset() {
    return thecluster;
  }
  
  /** The Transferable being sent
    * @param aTransferable
    */
  public void setTransferable(Transferable aTransferable) {
    thetransferable = aTransferable;
  }
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Cluster.
    * @param anAsset
    */
  public void setAsset(Asset anAsset) {
    // double check that this is an org asset for now
    if (anAsset.getClusterPG() != null) {
      thecluster = anAsset;
    } else {
      throw new IllegalArgumentException("TransferableTransfer.setAsset(anAsset) expects an Asset of with a clusterPG!");
    }
  }
  
}
