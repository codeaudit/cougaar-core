/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.core.plugin.completion;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.util.UnaryPredicate;

/**
 */
public class CompletionCalculator {

  protected static final UnaryPredicate TASK_PRED = 
    new UnaryPredicate() {
      public boolean execute(Object o) {
        return (o instanceof Task);
      }
    };

  protected UnaryPredicate pred;

  public UnaryPredicate getPredicate() {
    if (pred == null) {
      pred = createPredicate();
    }
    return pred;
  }

  public double calculate(Collection c) {
    int n = (c != null ? c.size() : 0);
    if (n <= 0) {
      return 1.0;
    }
    double sum = 0.0;
    if (c instanceof List) {
      List l = (List) c;
      for (int i = 0; i < n; i++) {
        Object o = l.get(i);
        sum += getConfidence(o);
      }
    } else {
      Iterator x = c.iterator();
      for (int i = 0; i < n; i++) {
        Object o = x.next();
        sum += getConfidence(o);
      }
    }
    return (sum / n);
  }

  protected UnaryPredicate createPredicate() {
    // need to count all tasks, even though we're only
    // interested in the tasks with alloc results.
    //
    // If this is changed then the completion servlet
    // must also be fixed!  The servlet assumes that
    // the basic "CompletionCalculator" predicate
    // matches all tasks.
    return TASK_PRED;
  }

  protected double getConfidence(Object o) {
    if (o instanceof Task) {
      Task task = (Task) o;
      PlanElement pe = task.getPlanElement();
      if (pe != null) {
        AllocationResult ar = pe.getEstimatedResult();
        if (ar != null) {
          double conf = ar.getConfidenceRating();
          conf = Math.min(conf/0.89999999, 1.0);
          return conf;
        }
      }
    }
    return 0.0;
  }

}
