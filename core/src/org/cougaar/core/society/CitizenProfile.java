/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.util.Vector;

/**
*  This class server as the persistent data structure that defines the current running
*   configuration of the Citizen.
*   <p>
*   Each Citizen has a profile that is persistent and a copy that is currently in use.
*
**/
public abstract class CitizenProfile {

  /** The Alias name for the Citizen that owns this profile. **/
  private String theAlias;
   
  /**
   *   Constructor no parametes.  Creates a Citizen's Profile with the
   *   @see org.cougaar.core.society.Citizen
   **/
  public CitizenProfile( String anAlias ){
    setAlias( anAlias );
  }

  /** 
   *	Accessor method for theAlias attribute.
   *   <p>
   *   @return String The String object that is referenced by theAlias attribute.    
   **/
  public final String getAlias() { return theAlias; }

  /** 
   *	Modifier method for theAlias attribute.
   *   <p>
   *   @param anAlias The String object to reference with theAlias attribute.   
   **/
  public final void setAlias( String anAlias ) { theAlias = anAlias; }

  /**
   *	The abstracted modifier method that takes a key and a value from the .ini file and
   *	maps it to the correct modifier method.  Ultimate this should be part of the application level
   *	configuration component and we should merely ask the running application for these values.
   *	<p>
   *	@param sKey  The String object that contains the key value from the ini file.
   *	@param sValue  The String object that contains the key value from the ini file.
   **/
  protected void setValue( String sKey, String sValue ) {
    if( sKey.equals( "policy") ) {
    } 
    else if( sKey.equals( "permission") ) {
    }
    else if( sKey.equals( "operation" ) ) {
    }
  }
    
}


