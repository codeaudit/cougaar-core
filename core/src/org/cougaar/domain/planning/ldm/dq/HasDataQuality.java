/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.dq;

/** An interface to be implemented by all objects which support
 * DataQuality.
 * @see org.cougaar.domain.planning.ldm.dq.DataQuality
 * @see org.cougaar.domain.planning.ldm.dq.NewHasDataQuality
 **/

public interface HasDataQuality {
  /** @return the DataQuality instance associated with this 
   * instance.  May return null if the object has undefined quality, 
   * i.e. it supports DataQuality decoration but has not been so 
   * decorated.
   **/
  DataQuality getDataQuality();
}
