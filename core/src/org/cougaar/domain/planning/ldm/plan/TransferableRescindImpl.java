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

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.society.UID;
import org.cougaar.domain.planning.ldm.plan.NewTransferableRescind;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Transferable;
import org.cougaar.domain.planning.ldm.plan.TransferableRescind;
import org.cougaar.core.cluster.persist.PersistenceOutputStream;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/** TransferableRescind implementation
 * TransferableRescind allows a transferable to be rescinded from the Plan. 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TransferableRescindImpl.java,v 1.1 2000-12-15 20:16:45 mthome Exp $
 **/

public class TransferableRescindImpl 
  extends DirectiveImpl
  implements TransferableRescind, NewTransferableRescind
{

  private UID transferableUID = null;
        
  /**
   * Returns the transferable to be rescinded
   * @return Transferable
   */

  public UID getTransferableUID() {
    return transferableUID;
  }
     
  public void setTransferableUID(UID tuid) {
    transferableUID = tuid;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
  }

  public String toString() {
    return "<TransferableRescind for " + transferableUID + ">";
  }

}
