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

package org.cougaar.planning.ldm.asset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.RelationshipScheduleImpl;

public class RelationshipBG implements PGDelegate {
  protected transient NewRelationshipPG myPG;
  

  public RelationshipBG() {
  }

  public RelationshipBG(NewRelationshipPG pg, 
                        HasRelationships hasRelationships) {
    init(pg, hasRelationships);
  }

  
  public PGDelegate copy(PropertyGroup pg) { 
    if (!(pg instanceof NewRelationshipPG)) {
      throw new java.lang.IllegalArgumentException("Property group must be a RelationshipPG");
    }

    NewRelationshipPG relationshipPG = (NewRelationshipPG ) pg;

    if (relationshipPG.getRelationshipSchedule() != null) {
      return new RelationshipBG(relationshipPG, 
                                relationshipPG.getRelationshipSchedule().getHasRelationships());
    } else {
      return new RelationshipBG(relationshipPG, null);
    }
  }

  public void readObject(ObjectInputStream in) {
    try {
     in.defaultReadObject();

     if (in instanceof org.cougaar.core.persist.PersistenceInputStream){
       myPG = (NewRelationshipPG) in.readObject();
     } else {
       // If not persistence, need to initialize the relationship schedule
       myPG = (NewRelationshipPG) in.readObject();
       init(myPG, myPG.getRelationshipSchedule().getHasRelationships());
     }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException();
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
      throw new RuntimeException();
    }
  }       

  public void writeObject(ObjectOutputStream out) {
    try {
      // Make sure that it agrees with schedule
      out.defaultWriteObject();
      
      if (out instanceof org.cougaar.core.persist.PersistenceOutputStream) {
        out.writeObject(myPG);
      } else {
        // Clear schedule before writing out
        myPG.getRelationshipSchedule().clear();
        out.writeObject(myPG);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new RuntimeException();
    }
  }

  public void init(NewRelationshipPG pg, HasRelationships hasRelationships) {
    myPG = (NewRelationshipPG) pg;

    RelationshipScheduleImpl pgSchedule = (RelationshipScheduleImpl) pg.getRelationshipSchedule();
    if ((pgSchedule == null) ||
        (pgSchedule.isEmpty())){
      myPG.setRelationshipSchedule(new RelationshipScheduleImpl(hasRelationships));
    } else if (!pgSchedule.getHasRelationships().equals(hasRelationships)) {
       throw new java.lang.IllegalArgumentException("");
    }
  }

  public boolean isSelf() {
    return isLocal();
  }

  public boolean isLocal() {
    return myPG.getLocal();
  }
}



