/*
 *
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
package org.cougaar.planning.servlet;

import org.cougaar.planning.ldm.plan.AllocationResult;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.Verb;

/**
 * These are example "public static boolean" methods for use by the
 * <code>PlanViewServlet</code> as examples in the "Advanced Search".
 *
 * The file <tt>PlanViewServlet.DEFAULT_PRED_FILENAME</tt> uses these as
 * examples, such as:<pre>
 *   (org.cougaar.planning.servlet.PredExamples:examplePredicateA (this))
 * </pre>.
 */
public class PredExamples {

  /**
   * This is an example of a "public static boolean" predicate for the 
   * predicate search -- this one happens to check for a <code>Task</code>, 
   * but one could write arbitrarily complicated code here.
   *
   * This is here for the examples only!  Other "utility" predicates should
   * be placed in a different class (e.g. "SearchUtils")!
   */
  public static boolean examplePredicateA(Object o) {
    return (o instanceof Task);
  }

  /** @see #examplePredicateA(Object) */
  public static boolean examplePredicateB(
      Task t, 
      String verbStr) {
    return t.getVerb().equals(verbStr);
  }

  /** @see #examplePredicateA(Object) */
  public static boolean examplePredicateC(
      Object o, 
      String verbStr, 
      double minConf) {
    if (o instanceof Task) {
      Task t = (Task)o;
      if ((verbStr == null) ||
          (t.getVerb().equals(verbStr))) {
        PlanElement pe = t.getPlanElement();
        if (pe != null) {
          AllocationResult est = pe.getEstimatedResult();
          return 
            ((est != null) &&
             (est.getConfidenceRating() >= minConf));
        }
      }
    }
    return false;
  }
}
