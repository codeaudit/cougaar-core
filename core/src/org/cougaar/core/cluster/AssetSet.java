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

import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.Asset;

/**
 * AssetSet is a custom container which maintains a hashtable-like
 * association between asset's item id and the asset object.  
 **/

public class AssetSet 
  extends KeyedSet
{
  protected Object getKey(Object o) {
    return ((Asset) o).getKey();
  }

  // special methods for Asset searches

  public Asset findAsset(Asset asset) {
    Object key = getKey(asset);
    if (key == null) return null;
    return (Asset) inner.get(key);
  }

  public Asset findAsset(String key) {
    return (Asset) inner.get(key);
  }

}




