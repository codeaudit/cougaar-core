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

package org.cougaar.core.plugin;

import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.Enumeration;

/**
 * A provider of prototype Assets to the LDM.
 * @see org.cougaar.core.plugin.LDMPlugInServesLDM
 * @author  ALPINE <alpine-software@bbn.com>
 **/

public interface PrototypeProvider extends LDMPlugInServesLDM {
  
  /** return the prototype Asset described by aTypeName.
   * implementations should probably call LDMServesPlugIn.cachePrototype
   * and LDMServesPlugIn.fillProperties if needed before returning.
   *
   * May return null if aTypeName is not something that the implementation
   * knows about.
   *
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
   * subclasses.
   **/
  Asset getPrototype(String aTypeName, Class anAssetClassHint);

  /** bulk version of getPrototype(String).
   * Will never return null.
   **/
  // Enumeration getPrototypes(Enumeration typeNames);
}
