/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.predicate;
import org.cougaar.domain.planning.ldm.plan.*;

/** Utility predicate for selecting PlanElements **/

public class PlanElementPredicate
  implements org.cougaar.util.UnaryPredicate
{
  public final boolean execute(Object o) {
    return (o instanceof PlanElement) && execute((PlanElement) o);
  }
  
  /** override this to select specific tasks **/
  public boolean execute(PlanElement o) { return true; }

}
