/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.util.Enumeration;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.util.StateModelException;

/** @deprecated Use SimplePlugIn
 **/

public abstract class ThinPlugIn extends PlugInAdapter
{
  public void load(Object object) throws StateModelException {
    chooseThreadingModel(SHARED_THREAD);
    super.load(object);
  }
}

