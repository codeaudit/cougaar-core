
package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.asset.Asset;

public class AssetAspectValue extends AspectValue
{
    private Asset theAsset;
    private boolean successBit = true;
    
    public AssetAspectValue(Asset anAsset, int type, double value)
    {
        super(type, value);
        this.theAsset = anAsset;
    }
    
    public Asset getAsset()
    {
        return theAsset;
    }
		
    public Object clone()
    {
        return new AssetAspectValue(theAsset, type, value);
    }
		
    public boolean getSuccessBit()
    {
        return successBit;
    }
    
    public void setSuccessBit(boolean value)
    {
        successBit = value;
    }
}
