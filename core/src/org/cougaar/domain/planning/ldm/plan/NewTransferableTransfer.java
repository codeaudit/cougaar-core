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

/** NewTransferableTransfer Interface
  * A Transferable Transfer should be used to transfer a Transferable object to
  * another cluster (org asset).  This interface provides setters to
  * build a complete object.
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: NewTransferableTransfer.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
  */
public interface NewTransferableTransfer extends TransferableTransfer {
  
  /** The Transferable being sent
    * @param aTransferable
    */
  void setTransferable(Transferable aTransferable);
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Cluster.
    * @param anAsset
    */
  void setAsset(Asset anAsset);
  
}
