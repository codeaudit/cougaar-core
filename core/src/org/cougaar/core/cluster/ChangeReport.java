/*
 * <copyright>
 *  Copyright 2000-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import java.io.Serializable;

/** A marker class describing a single change to an object (usually
 * a Publishable object).  Subclasses describe specific types of changes. <p>
 *
 * hashCode and equals methods should compute their return values based
 * on the type and not any specific (old) value which may be stored in the
 * object.  This allows use of set operations on ChangeReport instances to
 * filter out redundant reports. <p>
 *
 * Implementations should
 * be as compact as possible and must be Serializable so that they are
 * transferrable between distributed clusters and persistable. <p>
 *
 * @see org.cougaar.domain.planning.ldm.plan.Publishable
 **/

public interface ChangeReport 
  extends Serializable
{
}
