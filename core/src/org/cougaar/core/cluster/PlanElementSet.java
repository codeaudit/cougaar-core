/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import java.util.*;
import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.domain.planning.ldm.plan.*;

import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.society.UID;

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
