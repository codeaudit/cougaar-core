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

/* hand generated! */
package org.cougaar.domain.planning.ldm.asset;

import org.cougaar.domain.planning.ldm.asset.RelationshipBG;
import org.cougaar.domain.planning.ldm.asset.NewRelationshipPG;
import org.cougaar.domain.planning.ldm.asset.RelationshipPGImpl;

import org.cougaar.domain.planning.ldm.plan.HasRelationships;

public abstract class EntityAdapter extends Asset {


  public EntityAdapter() { 
    super();
  }

  protected EntityAdapter(EntityAdapter prototype) {
    super(prototype);
  }

  public Object clone() throws CloneNotSupportedException {
    EntityAdapter clone = (EntityAdapter) super.clone();
    clone.initRelationshipSchedule();
    return clone;
  }

  public void initRelationshipSchedule() {
    NewRelationshipPG relationshipPG = 
      (NewRelationshipPG) PropertyGroupFactory.newRelationshipPG();
    relationshipPG.setRelationshipBG(new RelationshipBG(relationshipPG, (HasRelationships) this));
    setRelationshipPG(relationshipPG);
  }
}



















