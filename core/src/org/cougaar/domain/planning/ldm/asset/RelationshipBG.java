/*--------------------------------------------------------------------------
 *                         RESTRICTED RIGHTS LEGEND
 *
 *   Use, duplication, or disclosure by the Government is subject to
 *   restrictions as set forth in the Rights in Technical Data and Computer
 *   Software Clause at DFARS 52.227-7013.
 *
 *                             BBNT Solutions LLC,
 *                             10 Moulton Street
 *                            Cambridge, MA 02138
 *                              (617) 873-3000
 *
 *   Copyright 2000 by
 *             BBNT Solutions LLC,
 *             all rights reserved.
 *
 * --------------------------------------------------------------------------*/
package org.cougaar.domain.planning.ldm.asset;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.RelationshipScheduleImpl;

public class RelationshipBG implements PGDelegate {
  protected transient NewRelationshipPG myPG;
  
  public RelationshipBG(NewRelationshipPG pg, 
                        HasRelationships hasRelationships) {
    myPG = (NewRelationshipPG) pg;

    RelationshipScheduleImpl pgSchedule = (RelationshipScheduleImpl) pg.getRelationshipSchedule();
    if ((pgSchedule == null) ||
        (pgSchedule.isEmpty())){
      init(hasRelationships);
    } else if (!pgSchedule.getHasRelationships().equals(hasRelationships)) {
      throw new java.lang.IllegalArgumentException("");
    }

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

     if (in instanceof org.cougaar.core.cluster.persist.PersistenceInputStream){
       myPG = (NewRelationshipPG) in.readObject();
     } else {
       // If not persistence, need to initialize the relationship schedule
       myPG = (NewRelationshipPG) in.readObject();
       init(myPG.getRelationshipSchedule().getHasRelationships());
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
      
      if (out instanceof org.cougaar.core.cluster.persist.PersistenceOutputStream) {
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

  public void init(HasRelationships hasRelationships) {
    myPG.setRelationshipSchedule(new RelationshipScheduleImpl(hasRelationships));
  }

  public boolean isSelf() {
    return isLocal();
  }

  public boolean isLocal() {
    return myPG.getLocal();
  }
}



