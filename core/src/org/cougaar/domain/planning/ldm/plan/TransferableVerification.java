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

import org.cougaar.core.society.UID;

/** Directive to verify Transferables
 * 
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TransferableVerification.java,v 1.2 2001-04-05 19:27:22 mthome Exp $
 **/
public interface TransferableVerification extends Directive{
  public UID getTransferableUID();
}
