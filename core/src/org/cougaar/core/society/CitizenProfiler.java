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

import java.util.Enumeration;

/**
*   This class is responsible for handling all configuration issues for the Citizen.
*   This is the class resposible changing the persistent state of a citizen.
**/
public abstract class CitizenProfiler extends Object{

  /** The reference to the persistent profile. **/
  private CitizenProfile theProfile = null;

  /**
   *   Default Constructor.
   **/
  public CitizenProfiler( ){
  }
    
  /**
   *   Accessor method for theProfile.
   *   <p>
   *   @return CitizenProfile The object pointed to by theProfile variable.
   **/
  public  CitizenProfile getProfile() { return theProfile; }

  /**
   *   Modifier method for theProfile property.
   *   <p>
   *   @param aProfile The CitizenProfile object to reference with theProfile member variable.
   **/
  protected final void setProfile( CitizenProfile aProfile ) { theProfile = aProfile; }


}



