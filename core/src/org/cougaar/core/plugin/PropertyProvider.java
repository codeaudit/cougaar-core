/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.Enumeration;

/**
 * A provider of Asset Properties to the LDM.
 * @see org.cougaar.core.plugin.LDMPlugInServesLDM
 **/

public interface PropertyProvider extends LDMPlugInServesLDM {
  
  /** Notify provider about a newly created asset.
   *
   * Should ignore assets which we know nothing about.
   *
   * Should block for as short a time as possible, perhaps
   * using fancy "future" propertygroups, etc.
   **/
  void fillProperties(Asset anAsset);

  /** bulk version of fillProperties(Asset)
   **/
  // void fillProperties(Enumeration assets);
}
