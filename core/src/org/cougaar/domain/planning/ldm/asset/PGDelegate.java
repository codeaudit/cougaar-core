/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.asset;

import java.io.Serializable;

public interface PGDelegate 
  extends Serializable 
{
  /** @return a copy of the current object for use in the
   * similar PropertyGroup passed as an argument.
   **/
  PGDelegate copy(PropertyGroup pg);
}
