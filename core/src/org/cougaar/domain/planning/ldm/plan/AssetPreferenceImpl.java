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
