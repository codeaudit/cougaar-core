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

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.asset.Asset;

/** An AspectValue that deals with Asset-Quantity relationships
 * Note that the Asset is probably a prototype
 *
 * @author  ALPINE <alpine-software@bbn.com>
 *
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
