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
import org.cougaar.domain.planning.ldm.plan.Transferable;

/** TransferableTransfer Interface
  * A Transferable Transfer should be used to transfer a Transferable object to
  * another cluster (org asset).
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: TransferableTransfer.java,v 1.2 2001-04-05 19:27:21 mthome Exp $
  */
public interface TransferableTransfer {
  
  /** The Transferable being sent
    * @return Transferable
    */
  Transferable getTransferable();
  
  /** The Asset the transferable is being sent to.  For now
    * the Assets should always be of type Organization, representing
    * another Cluster.
    * @return Asset
    */
  Asset getAsset();
  
}
  
