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

import org.cougaar.core.society.UID;

/** Directive to verify Transferables
 * 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TransferableVerificationImpl.java,v 1.1 2000-12-15 20:16:45 mthome Exp $
 **/
public class TransferableVerificationImpl
  extends DirectiveImpl
  implements TransferableVerification, NewTransferableVerification
{
  private UID uid;

  public TransferableVerificationImpl(Transferable t) {
    setTransferableUID(t.getUID());
  }

  public UID getTransferableUID() {
    return uid;
  }

  public void setTransferableUID(UID newUID) {
    uid = newUID;
  }
}
