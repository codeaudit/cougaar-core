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

/** Utility predicate for selecting (end state) Dispositions.
 * Note that it is a bad idea to write a multi-shot predicate to select for
 * failed Dispositions, since the failure status might change.
 **/

public class DispositionPredicate
  implements org.cougaar.util.UnaryPredicate
{
  public final boolean execute(Object o) {
    return (o instanceof Disposition) && execute((Disposition) o);
  }
  
  /** override this to select specific failedDispositions **/
  public boolean execute(Disposition o) { return true; }

}
