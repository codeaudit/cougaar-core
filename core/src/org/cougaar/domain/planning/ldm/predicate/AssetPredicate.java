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
import org.cougaar.domain.planning.ldm.asset.*;

/** Utility predicate for selecting assets **/

public class AssetPredicate
  implements org.cougaar.util.UnaryPredicate
{
  public final boolean execute(Object o) {
    return (o instanceof Asset) && execute((Asset) o);
  }
  
  /** override this to select specific Assets **/
  public boolean execute(Asset o) { return true; }

}
