/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterContext;

/**
 * LDM instances can serve many different sorts of clients.
 * this api is the commonality.
 *
 * @see org.cougaar.domain.planning.ldm.LDMServesPlugIn
 * @see org.cougaar.core.cluster.ClusterServesLogicProvider
 **/

public interface LDMServesClient 
  extends ClusterContext
{
  /** the (internal) time to mean unspecified **/
  public static final long UNSPECIFIED_TIME = 0L;

  /** @return the Root LDM Factory.  Equivalent to
   * (RootFactory)getFactory("Root") except will be somewhat more efficient.
   **/
  RootFactory getFactory();

  /** @return the Requested Domain's LDM Factory.
   **/
  Factory getFactory(String domain);

  /** Backwards Compatability (pre ALP6.0) alias for getFactory().
   * @deprecated Use getFactory()
   **/
  RootFactory getLdmFactory();

  /** @return the classloader to be used for loading classes for the LDM.
   * Domain PlugIns should not use this, as they will have been themselves
   * loaded by this ClassLoader.  Some infrastructure components which are
   * not loaded in the same way will require this for correct operation.
   **/
  ClassLoader getLDMClassLoader();
}
