/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;
import org.cougaar.core.cluster.*;

import java.net.URL;
public interface RemoteClusterMetrics
{
  // Use string, for quick transport, parsing
  // Each message will include cluster name, timestamp, -cnt, +cnt, LPCount
  public int receive( String obj ); 
}
