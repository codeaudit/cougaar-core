/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
    return true;
  }
}
