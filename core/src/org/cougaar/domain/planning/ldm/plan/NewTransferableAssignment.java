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


/**
  * TransferableAssignment Setter Interface
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: NewTransferableAssignment.java,v 1.2 2001-04-05 19:27:17 mthome Exp $
  **/
	
public interface NewTransferableAssignment extends TransferableAssignment, NewDirective  {
		
  /** @param newtransferable - sets the transferable being assigned */
  void setTransferable(org.cougaar.domain.planning.ldm.plan.Transferable newtransferable);
}
