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
