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

