/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.cluster.ClusterIdentifier;

/** Transferable 
 * @author   ALPINE <alpine-software@bbn.com>
 * @version  $Id: Transferable.java,v 1.2 2001-04-05 19:27:21 mthome Exp $
 *
 * Interface that describes the methods an object needs to be
 * transfered from one cluster to another using the Transferable Logic
 * Providers
 **/
public interface Transferable extends Cloneable, UniqueObject {
  /** A Transferable must be fully cloneable, otherwise unwanted side effects
   * may show up when object replicas are on clusters in the same VM
   **/
  public Object clone();

  /** 
   * A "close enough" version of equals() used by the Logic Provider
   * to find the local version of an object transfered from another cluster
   **/
  public boolean same(Transferable other);

  /**
   * Set all relevent parameters to the values in other.
   * Almost a deep copy.
   * @param other - must be of same type as this
   **/
  public void setAll(Transferable other);

  public boolean isFrom(ClusterIdentifier src);
}
