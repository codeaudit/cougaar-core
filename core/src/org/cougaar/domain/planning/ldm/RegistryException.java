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
