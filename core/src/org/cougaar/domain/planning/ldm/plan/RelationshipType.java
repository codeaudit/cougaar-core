/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
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
  

