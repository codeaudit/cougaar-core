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
*   This interface contains all the final error types for BindingException.
*	Any class that must construct or evaluate a bindingException implements this interface.
*   @author  Bob Peterson (Raytheon)
*   @version 0.0.0.0
*   @see RegistryException
**/
public interface RegistryExceptionIfc {
    /** Final int for check that the error type is with in acceptable boundaries**/
    public final static int REGISTRY_ERROR_BEGIN = 0;
    /** Final static variable for an Unknown Type Binding Exception **/
    public final static int REGISTRY_ERROR_UNKNOWN = 0;
    /** Final static variable for an attempt to remove the last canonical concept name **/
    public final static int REGISTRY_ERROR_REMOVE_LAST_CANONICAL_NAME = 1;
    /** Final static varaible for an error of creating a second root **/
    public final static int REGISTRY_ERROR_SECOND_ROOT = 2;
    /** Final static varaible for an error of creating a second root **/
    public final static int REGISTRY_ERROR_PARENT_TERM_NOT_CANONICAL = 3;
    /** Final static varaible for an error of creating a second root **/
    public final static int REGISTRY_ERROR_PARENT_TERM_NOT_FOUND = 4;
    /** Final static varaible for an error of creating a second root **/
    public final static int REGISTRY_ERROR_INTERNAL_INCONSISTENCY_FAILURE = 5;
    /** Final static varaible for an error of creating a second root **/
    public final static int REGISTRY_ERROR_PARSING_ERROR = 6;
    /** Final static variable for a Logical Data Model Binding Exception **/
    public final static int REGISTRY_ERROR_INCOMPLETE_REGISTRY_FILE_SECTION = 7;
    /** Final static variable for a Logical Data Model Binding Exception **/
    public final static int REGISTRY_ERROR_NAME_NOT_FOUND = 8;
    /** Final static variable for a Logical Data Model Binding Exception **/
    public final static int REGISTRY_ERROR_NAME_ALREADY_DEFINED = 9;
    /** Final int for check that the error type is with in acceptable boundaries
    *   This should alway be equal to the highest posssbile error code**/
    public final static int REGISTRY_ERROR_END = 9;

}
