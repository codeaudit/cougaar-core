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

import java.util.HashMap;

/**
 * Relationship - maps relationship between any two objects
 * Role describes the Role the direct object is performing for the 
 * indirect object.
 * BOZO think up better terms than direct/indirect object
 **/

public class RelationshipType {
  private static HashMap myTypes = new HashMap(3);

  public static RelationshipType create(String firstSuffix,
                                        String secondSuffix) {

    RelationshipType existing = get(firstSuffix);

    if (existing == null) {
      return new RelationshipType(firstSuffix, secondSuffix);
    } else if ((existing.getFirstSuffix().equals(firstSuffix)) &&
               (existing.getSecondSuffix().equals(secondSuffix))) {
      return existing;
    } else {
      throw new java.lang.IllegalArgumentException("First suffix " +
                                                   firstSuffix + " or " + 
                                                   " second suffix " + 
                                                   secondSuffix + 
                                                   " already used in - " +
                                                   existing);
    }
    

  }

  public static RelationshipType get(String suffix) {
    return (RelationshipType) myTypes.get(suffix);
  }


  private String myFirstSuffix;
  private String mySecondSuffix;

  public String getFirstSuffix() {
    return myFirstSuffix;
  }

  public String getSecondSuffix() {
    return mySecondSuffix;
  }

  public String toString() {
    return myFirstSuffix + ", " + mySecondSuffix;
  }

  private RelationshipType(String firstSuffix, String secondSuffix) {
    myFirstSuffix = firstSuffix;
    mySecondSuffix = secondSuffix;
    
    myTypes.put(firstSuffix, this);
    myTypes.put(secondSuffix, this);
  }

  public static void main(String []args) {
    create("Superior", "Subordinate");
    create("Provider", "Customer");
    //create("aaa", "Customer");
    //create("Superior", "bbb");

    System.out.println("Match on chocolate - " + get("chocolate"));
    System.out.println("Match on Superior - " + get("Superior"));
    System.out.println("Match on Customer - " + get("Customer"));
    
  }
}
  

