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

package org.cougaar.core.domain;

import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;

public final class Registry  {

  /** THEINITIALREGISTRYSIZE specifies the initial size of the HashMap theRegistry,
   *   which contains the Strings of RegistryTerms
   **/
  private final static int THE_INITIAL_REGISTRY_SIZE = 89 ;

  public int size() { return theRegistry.size(); }
    
  /**
   *   When a client requests retrieval of the RegistryTerm instance using
   *   a Domain Name as the lookup key, this HashMap is used to execute 
   *   the lookup.
   **/
  private final HashMap theRegistry = new HashMap(THE_INITIAL_REGISTRY_SIZE);

  public Registry() {}
    
  /** 
   *   Retrieve the RegistryTerm containing a particular Domain Name 
   *   @param aDomainName The String containing the Domain Name to be looked up
   *   @return Search succeeded: A RegistryTerm representing the Domain Name.
   *   Search failed: null 
   **/
  public synchronized Object findDomainName( String aDomainName ) {
    Object o = theRegistry.get(aDomainName);
    return o;
  }

  /** 
   *  Method to create a new RegistryTerm.
   *  @param aDomainName The string containing the name.
   *  @param aRegistryConcept A reference to the concept this name refers to.
   *  @exception RegistryException If connecting the name to the concept fails
   **/
  public synchronized Object createRegistryTerm(String aDomainName, Object aRegistryConcept)
  {
    theRegistry.put( aDomainName.intern(), aRegistryConcept);
    return aRegistryConcept;
  }

  // backup uppercase-only registry for debugging stupid code.
  //private final HashMap theOtherRegistry = new HashMap(89);
}