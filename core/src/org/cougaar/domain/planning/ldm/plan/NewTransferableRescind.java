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

/** NewTransferableRescind Interface
 * Provides setter methods for object creation 
 **/

public interface NewTransferableRescind extends TransferableRescind, NewDirective 
{
  /**
   * Sets the UID of the transferable to be rescinded
   * @param uid - The UID of the Transferable to be rescinded.
   **/
  void setTransferableUID(UID uid);
}
