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
// Source file: LDM/AggregateAsset.java
// Subsystem: LDM
// Module: AggregateAsset


package org.cougaar.domain.planning.ldm.asset ;

import java.util.Enumeration;
import java.util.Date;

import java.util.Vector;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;

public class AggregateAssetAdapter extends Asset  {
  private transient Asset myAsset;
  private long thequantity;
    
  AggregateAssetAdapter() { }

  AggregateAssetAdapter(AggregateAssetAdapter prototype) {
    super(prototype);
    myAsset = prototype.getAsset();
  }

  public Asset getAsset() {
    return myAsset;
  }

  public void setAsset(Asset arg_Asset) {
    myAsset= arg_Asset;
  }

  public long getQuantity() {
    return thequantity;
  }
  
  void setQuantity(long quantity){
    thequantity = quantity;
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    myAsset = (Asset) stream.readObject();
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(myAsset);
  }

  private static PropertyDescriptor properties[];
  static {
    try {
      properties = new PropertyDescriptor[2];
      properties[0] = new PropertyDescriptor("Asset", AggregateAssetAdapter.class, "getAsset", null);
      properties[1] = new PropertyDescriptor("Quantity", AggregateAssetAdapter.class, "getQuantity", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pds = super.getPropertyDescriptors();
    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+properties.length];
    System.arraycopy(pds, 0, ps, 0, pds.length);
    System.arraycopy(properties, 0, ps, pds.length, properties.length);
    return ps;
  }

  public int hashCode() {
    int hc = 0;
    if (myAsset != null) hc=myAsset.hashCode();
    hc += thequantity;
    return hc;
  }

  /** Equals for aggregate assets is defined as having the
   * same quantity of the same (equals) asset.  TID and IID are
   * ignored.
   **/
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(getClass() == o.getClass())) return false;
    AggregateAssetAdapter oaa = (AggregateAssetAdapter) o;
    if (myAsset != null && !(myAsset.equals(oaa.getAsset()))) return false;
    if (thequantity != oaa.getQuantity()) return false;
    ItemIdentificationPG pg1 = getItemIdentificationPG();
    String id1 = (pg1 ==null)?null:pg1.getItemIdentification();
    ItemIdentificationPG pg2 = oaa.getItemIdentificationPG();
    String id2 = (pg2 ==null)?null:pg2.getItemIdentification();

                                // return true IFF
    return (id1 != null &&      // both have non-null item ids
            id1.equals(id2)     //  which are .equals
            );
  }
}
