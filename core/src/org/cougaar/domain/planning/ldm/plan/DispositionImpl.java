/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.beans.*;
import java.util.*;


/** 
 * Implementation for Disposition
 */
 
public class DispositionImpl extends PlanElementImpl 
  implements Disposition {

  public DispositionImpl() {}

  /* Constructor that takes the failed result
   * @param p
   * @param t
   * @param result
   * @return FailedDisposition
   */
  public DispositionImpl(Plan p, Task t, AllocationResult result) {
    super(p, t);
    estAR = result;
  }

  public boolean isSuccess() {
    return (estAR != null && estAR.isSuccess());
  }

  public String toString() {
    return ("[Disposition #" + getTask().getUID() + "]");
  }

}
