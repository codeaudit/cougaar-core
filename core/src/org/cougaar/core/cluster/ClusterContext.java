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

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.society.Message;
import org.cougaar.domain.planning.ldm.Registry;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

import java.io.*;

/**
 * Interface required for out-of-band communication with clusters.
 * This is a privileged access point interface which only be used
 * by internal cluster mechanisms.
 **/

public interface ClusterContext
{
  /** The current cluster's CID */
  ClusterIdentifier getClusterIdentifier();
  
  UIDServer getUIDServer();

  LDMServesPlugIn getLDM();

  public static final class DummyClusterContext implements ClusterContext {
    private static final ClusterIdentifier cid = new ClusterIdentifier("_Dummy");
    public ClusterIdentifier getClusterIdentifier() { return cid; }
    public UIDServer getUIDServer() { return null; }
    public LDMServesPlugIn getLDM() { return null; }
  }
}
