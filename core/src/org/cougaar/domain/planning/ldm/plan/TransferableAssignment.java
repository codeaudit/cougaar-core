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

/** Directive to transfer Transferables
 * 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TransferableAssignment.java,v 1.2 2001-04-05 19:27:21 mthome Exp $
 **/
public interface TransferableAssignment extends Directive{

  public Transferable getTransferable();
}
