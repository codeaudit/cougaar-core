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

import org.cougaar.domain.planning.ldm.plan.Transferable;
import org.cougaar.domain.planning.ldm.plan.TransferableAssignment;
import org.cougaar.domain.planning.ldm.plan.NewTransferableAssignment;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.cluster.ClusterIdentifier;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/** 
 * Directive message containing a Transferable 
 **/

public class TransferableAssignmentImpl 
  extends DirectiveImpl
  implements TransferableAssignment, NewTransferableAssignment
{
  private transient Transferable assignedTransferable;

  public TransferableAssignmentImpl() {
    super();
  }

  public TransferableAssignmentImpl(Transferable transferable) {
    assignedTransferable = transferable;
  }

  public TransferableAssignmentImpl(Transferable transferable, ClusterIdentifier src, 
			      ClusterIdentifier dest) {
    assignedTransferable = transferable;
    super.setSource(src);
    super.setDestination(dest);
  }

  /** implementations of the TransferableAssignment interface */
		
  /** @return transferable that has beeen assigned */
  public Transferable getTransferable() {
    return assignedTransferable;
  }

  /** implementation methods for the NewTransferableAssignment interface */
  /** @param newtransferable sets the transferable being assigned */
  public void setTransferable(Transferable newtransferable) {
    assignedTransferable = newtransferable;
  }


  public String toString() {
    String transferableDescr = "(Null AssignedTransferable)";
    if( assignedTransferable != null ) transferableDescr = assignedTransferable.toString();

    return "<TransferableAssignment "+transferableDescr+", " + ">" + super.toString();
  }


  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(assignedTransferable);
  }



  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();
    assignedTransferable = (Transferable)stream.readObject();
  }



}
