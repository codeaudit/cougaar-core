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

import org.cougaar.planning.ldm.plan.Transferable;
import org.cougaar.planning.ldm.plan.TransferableAssignment;
import org.cougaar.planning.ldm.plan.NewTransferableAssignment;
import org.cougaar.core.blackboard.Directive;
import org.cougaar.core.blackboard.DirectiveImpl;
import org.cougaar.core.mts.MessageAddress;

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

  public TransferableAssignmentImpl(Transferable transferable, MessageAddress src, 
			      MessageAddress dest) {
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
