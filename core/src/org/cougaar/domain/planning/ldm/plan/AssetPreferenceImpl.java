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

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.PreferenceImpl;

public class AssetPreferenceImpl extends PreferenceImpl
	implements Cloneable 
{
    Asset theAsset;
    
    public AssetPreferenceImpl()
    {
        super();
    }
    
    public AssetPreferenceImpl (Asset anAsset)
    {
        super();
        theAsset = anAsset;
    }
	
    public Asset getAsset()
    {
        return theAsset;
    }
	
    public void setAsset(Asset anAsset)
    {
        theAsset = anAsset;
    }
	
    public Object clone()
    {
        return new AssetPreferenceImpl(getAsset());
    }
}
