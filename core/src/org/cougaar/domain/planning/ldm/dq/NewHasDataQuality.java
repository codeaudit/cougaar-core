/*
 * <copyright>
 *  Copyright 2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.dq;

/** An interface to be implemented by all objects which support
 * DataQuality.
 * @see org.cougaar.domain.planning.ldm.dq.DataQuality
 * @see org.cougaar.domain.planning.ldm.dq.HasDataQuality
 **/

public interface NewHasDataQuality extends HasDataQuality {
  /** Set the DataQuality description to be associated with
   * a given HasDataQuality instance.
   **/
  void setDataQuality(DataQuality dq);
}
