/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm;

/*
*   Imports
*/


/**
*   This Exception class contains the information for throwing an
*   exception for Registry operations.
*   <p>
*   For the first version of this class the exceptions diffferentiated by the
*   int type passed in during construction.  In the future we will want
*   to break this class out into specifc kinds of RegistryExceptions exceptions
*   @author  Bob Peterson (Raytheon)
*   @version 0.0.0.0
*   @see RegistryExceptionIfc
**/
public class RegistryException extends RuntimeException implements RegistryExceptionIfc {

    /** Variable containing the type of Exception thrown **/
    private int theType;


    /**
    *   Constructs an RegistryException with no specified detail message.
    *   @param aType Argument used to set the type of error condition that occurred
    *   @return RegistryException
    **/
    public RegistryException( int aType ) {
            this( aType , "" );
    }

    /**
    *   Constructs a RegistryException with the specified detail message.
    *   @param aType  Argument used to set the type of error condition that occurred
    *   @param aMsg    Argument used to set a specific eddetail message
    *   @return RegistryException
    **/
    public RegistryException(int aType, String aMsg ) {
            super(aMsg);
            setType( aType );
    }

    /**
    *   Accessor for theType variable
    *   @return int The spepeciifcerror code for this exception see the class variables above
    **/
    public int getType() { return theType; }

    /**
    *   Modifier for theType variable.  If the value is outside of the allowable error code range then
    *   we call it a REGISTRY_ERROR_UNKNOWN type of error
    *   @param aType An integer value that contains one of the possible class values above
    **/
    public void setType( int aType ) {
        if( aType < REGISTRY_ERROR_BEGIN || aType > REGISTRY_ERROR_END )
            aType = REGISTRY_ERROR_UNKNOWN;
        theType = aType;
    }

    /**
    *   Overrides the supers toString to add the error type to the string
    *   @return String The string representationof this object
    **/
    public String toString(){
        return super.toString() + " " + String.valueOf( getType() ) ;
    }
}
