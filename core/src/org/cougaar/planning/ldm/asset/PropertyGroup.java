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
package org.cougaar.planning.ldm.asset;

import java.io.Serializable;

public interface PropertyGroup extends Serializable, Cloneable {

  Object clone() throws CloneNotSupportedException;

  /** Unlock the PropertyGroup by returning an object which
   * has setter methods that side-effect this object.
   * The key must be == the key that locked the property
   * in the first place or an Exception is thrown.
   * @exception IllegalAccessException
   **/
  NewPropertyGroup unlock(Object key) throws IllegalAccessException;

  /** lock a property by returning an immutable object which
   * has a private view into the original object.
   * If key == null, the result is a locked object which cannot be unlocked.
   **/
  PropertyGroup lock(Object key);

  /** alias for lock(null)
   **/
  PropertyGroup lock();

  /** Convenience method. equivalent to clone();
   **/
  PropertyGroup copy();

  /** returns the class of the main property interface for this 
   * property group.  
   **/
  Class getPrimaryClass();

  /** @return the method name on an asset to retrieve the PG **/
  String getAssetGetMethod();
  /** @return the method name on an asset to set the PG **/
  String getAssetSetMethod();


  // DataQuality
  /** @return true IFF the instance not only supports DataQuality
   * queries (e.g. is instanceof HasDataQuality), but getDataQuality()
   * will return non-null.
   **/
  boolean hasDataQuality();
}
