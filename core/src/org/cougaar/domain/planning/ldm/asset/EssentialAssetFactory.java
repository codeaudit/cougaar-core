/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.asset;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.cougaar.domain.planning.ldm.*;
import org.cougaar.domain.planning.ldm.asset.ItemIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.domain.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;
import org.cougaar.domain.planning.ldm.asset.PropertyGroupFactory;
import org.cougaar.domain.planning.ldm.asset.TypeIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.TypeIdentificationPGImpl;

import org.cougaar.domain.planning.ldm.Registry;

/**
 *   This is the class that is responsible for generating a LDM object 
 * based upon a Data Dictionary string 
 */
public class EssentialAssetFactory {

  /**
   *   Constructor. Create a new instance of a the Factory.
   **/
  public EssentialAssetFactory( ) {}

  /** 
   *       Copy method for any ASSET object.
   *       @return ASSET object The object created by the factory returned as a unique instance
   *       @exception ASSETFactoryException If the method fails to create a new ASSET object .
   **/
  public Asset copy( Asset original, String aUniqueID ) throws FactoryException {
    // We need to clone it before treating it like a unique instance
    Asset anInstance = null;
    try {
      anInstance = (Asset)(original.clone());

      // Associate an ItemIdentifierProperty with the new Asset.
      // The clone already has it's own IIP, no need to cons a new one.
      //NewItemIdentificationPG iip = PropertyGroupFactory.newItemIdentificationPG();
      NewItemIdentificationPG iip = (NewItemIdentificationPG) anInstance.getItemIdentificationPG();
      iip.setItemIdentification( aUniqueID );
      iip.setNomenclature( aUniqueID );
      //iip.setAlternateItemIdentification( aUniqueID );
      //anInstance.setItemIdentificationPG( iip );
    } catch( CloneNotSupportedException ce ) {
      ce.printStackTrace();
      throw new FactoryException( "Clone not supported for " + aUniqueID + " " + original.getClass() );
    }

    return anInstance;
  }

  /**
   *       Method to create a new object of type Asset.
   *       This should only be called from the LDMFactory.
   *       @return Asset object The object created by the factory returned as an abstract object type
   *       @exception FactoryException If the method fails to create a new Asset .
   **/
  public Asset create( Class myClass, String aTypeID ) throws FactoryException {
    // Assign existing prototypes from the cache
    //  otherwise create a new prototype and add it to the cache
    // The dictionary name is required to find the correct instance of a
    // prototype object.
    Asset theAsset = null;
    try {
      theAsset = (Asset)myClass.newInstance();
      if( theAsset == null )
        throw new FactoryException( "Could not create the Data Model Object for class " + myClass );
            
      // Attach the Type Identification property here because
      //   1. We have the value, and
      //   2. We depend on the Type Identification value to form Domain Names for other Properties
      //        TypeIdentificationPGImpl tip = (TypeIdentificationPropertyImpl)theAsset.createTypeIdentificationProperty();

      String convertedID = aTypeID;
      if( aTypeID == null )
        convertedID = "RuntimePrototype";

      NewTypeIdentificationPG tip = (NewTypeIdentificationPG) theAsset.getTypeIdentificationPG();
      tip.setTypeIdentification( convertedID );
      tip.setNomenclature(convertedID);
      //theAsset.setTypeIdentificationPG( tip );  // All TypeIds are mutable!

      /*
      theAsset.setTypeIdentificationPG((TypeIdentificationPG) 
                                             tip.lock(getKey()));
      */
      theAsset.setTypeIdentificationPG(tip);
    } catch( Exception be ) {
      be.printStackTrace();
      throw new FactoryException("Exception caught while building " +
                                  aTypeID +
                                  " based on asset class: " + 
                                  myClass + 
                                  ".\n" + 
                                  be.toString() );
    }

    return theAsset;
  }

  /** return the assetFactory's property key **/
  private Object getKey() { return this; }

  public int hashCode() { return this.getClass().hashCode(); }
  public boolean equals(Object o) {
    return o != null && 
      ( this==o || this.getClass()==o.getClass());
  }
}
