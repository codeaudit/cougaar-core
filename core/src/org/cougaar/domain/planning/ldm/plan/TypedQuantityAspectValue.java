/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.asset.Asset;

/** An AspectValue that deals with Asset-Quantity relationships
 * Note that the Asset is probably a prototype
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: TypedQuantityAspectValue.java,v 1.1 2000-12-15 20:16:45 mthome Exp $
 */
 
public class TypedQuantityAspectValue extends AspectValue {
  private Asset theasset;
  
  /** Simple Constructor that takes the asset and the quantity.
   * @param anAsset  The Asset - probably a prototype
   * @param aQuantity  The amount of assets.
   * @return TypedQuantityAspectValue
   */
  public TypedQuantityAspectValue(Asset anAsset, double aQuantity) {
    super(AspectType.TYPED_QUANTITY, aQuantity);
    this.theasset = anAsset;
  }
   
  /** @return Asset The Asset represented by this aspect */
  public Asset getAsset() {
    return theasset;
  }
  
  public Object clone() {
    return new TypedQuantityAspectValue(theasset, value);
  }
  
}