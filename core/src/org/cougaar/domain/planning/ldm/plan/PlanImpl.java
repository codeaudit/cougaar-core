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
// import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.plan.Plan;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * A Plan is an abstract data structure which consists of
 * a set of PlanElements (which are associations between Tasks and
 *  Allocations).
 */

public final class PlanImpl 
  implements Plan, Cloneable, Serializable
{

  private String planname;

  //no-arg constructor
  public PlanImpl() {
    super();
  }

  //constructor that takes string name of plan
  public PlanImpl (String s) {
    if (s != null) s = s.intern();
    planname = s;
  }

  /**@return String Name of Plan */
  public String getPlanName() {
    return planname;
  }

  public boolean equals(Object p) {
    return (this == p ||
            (planname != null && p instanceof PlanImpl
             && planname.equals(((PlanImpl)p).getPlanName())));
  }


  public String toString() {
    if (planname != null)
      return planname;
    else
      return "(unknown plan)";
  }


  //private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
  //  stream.defaultReadObject();
  //  if (planname != null) planname = planname.intern();
  //}


  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();

    if (planname != null) planname = planname.intern();
  }

  public static final Plan REALITY = new PlanImpl("Reality");
} 

