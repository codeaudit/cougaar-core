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

import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.*;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import java.util.*;

/** 
 * This is the cluster context made available to a 
 * stateless plugin.
 **/

public interface PlugInContext
  extends PlugInDelegate 
{
  /** Generally called by a stateless plugin during 
   * PlugIn.initialize().  May be called no more
   * than once or a RuntimeException will be thrown.
   */
  void setState(PlugIn.State state);

  /**
   * @return the previously set state for this context if any.
   **/
  PlugIn.State getState();
}
