/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.ldm.asset;

import java.io.Serializable;

public interface PropertyGroup extends Serializable, Cloneable {

  public Object clone() throws CloneNotSupportedException;

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
