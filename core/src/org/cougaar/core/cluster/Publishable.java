/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import java.util.List;

/** Objects marked as Publishable may be published directly to the Plan.
 * Provides hooks for additional Plan-related functionality.
 *
 * NOTE: at some point, <em>only</em> Publishable objects will be
 * admitted to the logplan.  Initially, however, this requirement will
 * not be enforced: Publishing other objects will work, but Publishable
 * services will not be available.
 **/

public interface Publishable
{
  /**
   * Provide a hint to Persistence on how to handle this object. <p>
   *
   * Example?
   **/
  boolean isPersistable();
}

