/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;
import org.cougaar.domain.planning.ldm.plan.ClusterObjectFactory;

import java.util.Enumeration;

/**
 * PlugIns primary interface to the LDM.
 *
 * @see org.cougaar.core.plugin.LDMPlugInServesLDM
 **/

public interface LDMServesPlugIn extends LDMServesClient
{
  
  /** Request that a prototype be remembered by the LDM so that
   * getPrototype(aTypeName) is likely to return aPrototype
   * without having to make calls to PrototypeProvider.getPrototype(aTypeName).
   * Note that the lifespan of a prototype in the prototype registry may
   * be finite (or even zero!).
   * Note: this method should be used only by PrototypeProvider LDM PlugIns.
   **/
  void cachePrototype(String aTypeName, Asset aPrototype);

  /** is there a prototype with the specified name currently in
   * the prototype cache?
   **/
  boolean isPrototypeCached(String aTypeName);   

  /** find the prototype Asset named by aTypeName.  This service
   * will actually be provided by a PrototypeProvider via a call to
   * getPrototype(aTypeName).
   * It will return null if no prototype is found or can be created
   * with that name.
   * There is no need for a client of this method to call cachePrototype
   * on the returned object (that task is left to whatever prototypeProvider
   * was responsible for generating the prototype).
   *
   * Some future release might want to throw an exception if not found.
   * An example aTypeName: "NSN/12345678901234".
   *
   * The returned Asset will usually, but not always have a primary 
   * type identifier that is equal to the aTypeName.  In cases where
   * it does not match, aTypeName must appear as one of the extra type
   * identifiers of the returned asset.  PrototypeProviders should cache
   * the prototype under both type identifiers in these cases.
   *
   * @param aTypeName specifies an Asset description. 
   * @param anAssetClassHint is an optional hint to LDM plugins
   * to reduce their potential work load.  If non-null, the returned asset 
   * (if any) should be an instance the specified class or one of its
   * subclasses.  When null, each PrototypeProvider will attemt to decode
   * the aTypeName enough to determine if it can supply prototypes of that
   * type.
   **/
  Asset getPrototype(String aTypeName, Class anAssetClass);

  /** equivalent to getPrototype(aTypeName, null);
   **/
  Asset getPrototype(String aTypeName);

  /* bulk version of getPrototype(String).
   */
  // Enumeration getPrototypes(Enumeration typeNames);

  /** Notify LDM of a newly created asset.  This is generally for the use
   * of LDMPlugIns, but others may use it to request that propertygroups
   * of the new Asset be filled in from various data sources.
   **/
  void fillProperties(Asset anAsset);

  /* bulk version of fillProperties
   */
  //void fillProperties(Enumeration assets);

  /** Used by assets to activate LateBinding of PropertyGroups to Assets.
   * Called as late as possible when it is not yet known if there is
   * a PG for an asset.
   **/
  PropertyGroup lateFillPropertyGroup(Asset anAsset, Class pg, long time);
}
