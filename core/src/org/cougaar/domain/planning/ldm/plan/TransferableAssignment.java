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

/** Directive to transfer Transferables
 * 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TransferableAssignment.java,v 1.1 2000-12-15 20:16:45 mthome Exp $
 **/
public interface TransferableAssignment extends Directive{

  public Transferable getTransferable();
}
