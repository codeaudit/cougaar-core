/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.util.Enumeration;
import org.cougaar.core.cluster.SubscriptionWatcher;
import org.cougaar.util.StateModelException;

// could be abstract, but it is useful to be able to plug-in a 
// completely empty plugin.  If it were abstract, we'd declare the execute
// method abstract.
/** @deprecated Use SimplePlugIn
 **/

public abstract class SingleThreadedPlugIn extends PlugInAdapter
{
  public void initialize() throws StateModelException {
    super.initialize();
    chooseThreadingModel(SINGLE_THREAD);
  }
}

