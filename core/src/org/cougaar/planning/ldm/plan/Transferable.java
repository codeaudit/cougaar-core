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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.mts.MessageAddress;

/** Transferable 
 *
 * Interface that describes the methods an object needs to be
 * transfered from one cluster to another using the Transferable Logic
 * Providers
 **/
public interface Transferable extends Cloneable, UniqueObject {
  /** A Transferable must be fully cloneable, otherwise unwanted side effects
   * may show up when object replicas are on clusters in the same VM
   **/
  Object clone();

  /** 
   * A "close enough" version of equals() used by the Logic Provider
   * to find the local version of an object transfered from another cluster
   **/
  boolean same(Transferable other);

  /**
   * Set all relevent parameters to the values in other.
   * Almost a deep copy.
   * @param other - must be of same type as this
   **/
  void setAll(Transferable other);

  boolean isFrom(MessageAddress src);

  /**
   * @see #isFrom
   */
  MessageAddress getSource();
}
