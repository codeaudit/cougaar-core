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

import org.cougaar.core.society.UID;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.component.Service;

public interface UIDService extends Service {
  /** ClusterIdentifier of the proxy server.
   *  This might go away if we ever really separated proxy 
   * servers from clusters.
   **/
  ClusterIdentifier getClusterIdentifier();

 /** get the next Unique ID for the Proxiable object registry
   * at this server.
   * It is better for Factories to use the registerUniqueObject method.
   **/
  UID nextUID();

  /** assign a new UID to a unique object.
   **/
  UID registerUniqueObject(UniqueObject o);

}
