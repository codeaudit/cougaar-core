/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.agent;

import org.cougaar.core.service.*;

import org.cougaar.core.blackboard.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.mts.Message;
import org.cougaar.core.domain.Registry;
import org.cougaar.core.domain.LDMServesPlugin;
import org.cougaar.planning.ldm.plan.ClusterObjectFactory;

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

  LDMServesPlugin getLDM();

  final class DummyClusterContext implements ClusterContext {
    private static final ClusterIdentifier cid = new ClusterIdentifier("_Dummy");
    public ClusterIdentifier getClusterIdentifier() { return cid; }
    public UIDServer getUIDServer() { return null; }
    public LDMServesPlugin getLDM() { return null; }
  }
}
