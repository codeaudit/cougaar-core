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

package org.cougaar.planning.ldm.plan;

import java.util.Map;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.KeyedSet;

/**
 * PlanElementSet is a custom container which maintains a hashtable-like
 * association between pe.task.key and pe object.  The supports the single
 * most time-consuming operation in logplan lookups.
 **/

public class PlanElementSet
  extends KeyedSet
{
  protected Object getKey(Object o) {
    if (o instanceof PlanElement) {
      Task task = ((PlanElement)o).getTask();
      if (task == null) {
        throw new IllegalArgumentException("Invalid PlanElement (no task) added to a PlanElementSet: "+o);
      }
      return ((UniqueObject) task).getUID();
    } else {
      return null;
    }
  }

  // special methods for PlanElement searches

  public PlanElement findPlanElement(Task task) {
    UID sk = ((UniqueObject) task).getUID();
    return (PlanElement) inner.get(sk);
  }

  public PlanElement findPlanElement(UID key) {
    return (PlanElement) inner.get(key);
  }
}
